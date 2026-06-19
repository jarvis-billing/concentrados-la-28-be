package com.co.jarvis.service.impl;

import com.co.jarvis.dto.AdjustCreditRequest;
import com.co.jarvis.dto.DepositCreditRequest;
import com.co.jarvis.dto.MerchandiseReturnDto;
import com.co.jarvis.dto.MerchandiseReturnItemDto;
import com.co.jarvis.dto.ReturnFilterDto;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.entity.Billing;
import com.co.jarvis.entity.MerchandiseReturn;
import com.co.jarvis.entity.MerchandiseReturnItem;
import com.co.jarvis.entity.Presentation;
import com.co.jarvis.entity.Product;
import com.co.jarvis.entity.PurchaseInvoice;
import com.co.jarvis.entity.UserReference;
import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.enums.EPaymentType;
import com.co.jarvis.enums.EReturnStatus;
import com.co.jarvis.enums.EReturnType;
import com.co.jarvis.enums.ESale;
import com.co.jarvis.repository.BillingRepository;
import com.co.jarvis.repository.MerchandiseReturnRepository;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.repository.PurchaseInvoiceRepository;
import com.co.jarvis.service.ClientAccountService;
import com.co.jarvis.service.ClientCreditService;
import com.co.jarvis.service.InventoryService;
import com.co.jarvis.service.MerchandiseReturnService;
import com.co.jarvis.util.DateTimeUtil;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchandiseReturnServiceImpl implements MerchandiseReturnService {

    private final MerchandiseReturnRepository returnRepository;
    private final BillingRepository billingRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final ClientAccountService clientAccountService;
    private final ClientCreditService clientCreditService;
    private final MongoTemplate mongoTemplate;

    // ========== DEVOLUCIÓN DE VENTA ==========

    @Override
    @Transactional
    public MerchandiseReturnDto createSaleReturn(MerchandiseReturnDto dto, String userId) {
        log.info("MerchandiseReturnServiceImpl -> createSaleReturn");

        validateReturnItems(dto.getItems());

        // Buscar factura original
        Billing billing = findOriginalBilling(dto);

        // Calcular totales
        calculateReturnTotals(dto);

        // Construir y guardar la devolución
        MerchandiseReturn returnEntity = buildReturnEntity(dto, EReturnType.DEVOLUCION_VENTA, userId);
        returnEntity = returnRepository.save(returnEntity);

        // Marcar la factura original con la referencia de esta devolución
        billing.setHasReturn(true);
        if (billing.getReturnIds() == null) billing.setReturnIds(new ArrayList<>());
        billing.getReturnIds().add(returnEntity.getId());
        billingRepository.save(billing);

        // Ajustar stock: incrementar (los productos vuelven al inventario)
        updateStockForSaleReturn(dto.getItems(), returnEntity.getId(), userId);

        // Resolver implicaciones financieras
        resolveSaleReturnFinancials(dto, billing, returnEntity.getId(), userId);

        log.info("Devolución de venta creada: {}", returnEntity.getReturnNumber());
        return mapToDto(returnEntity);
    }

    // ========== DEVOLUCIÓN DE COMPRA ==========

    @Override
    @Transactional
    public MerchandiseReturnDto createPurchaseReturn(MerchandiseReturnDto dto, String userId) {
        log.info("MerchandiseReturnServiceImpl -> createPurchaseReturn");

        validateReturnItems(dto.getItems());

        // Buscar factura de compra original
        PurchaseInvoice invoice = findOriginalPurchaseInvoice(dto);

        // Enriquecer datos del proveedor desde la factura original si no vienen en el DTO
        if (dto.getSupplierId() == null && invoice.getSupplier() != null) {
            dto.setSupplierId(invoice.getSupplier().getId());
            dto.setSupplierName(invoice.getSupplier().getName());
        }

        // Calcular totales
        calculateReturnTotals(dto);

        // Construir y guardar la devolución
        MerchandiseReturn returnEntity = buildReturnEntity(dto, EReturnType.DEVOLUCION_COMPRA, userId);
        returnEntity = returnRepository.save(returnEntity);

        // Ajustar stock: decrementar (los productos salen del inventario)
        updateStockForPurchaseReturn(dto.getItems(), returnEntity.getId(), userId);

        log.info("Devolución de compra creada: {}", returnEntity.getReturnNumber());
        return mapToDto(returnEntity);
    }

    // ========== CONSULTAS ==========

    @Override
    public MerchandiseReturnDto findById(String id) {
        log.info("MerchandiseReturnServiceImpl -> findById: {}", id);
        MerchandiseReturn entity = returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada con ID: " + id));
        return mapToDto(entity);
    }

    @Override
    public MerchandiseReturnDto findByReturnNumber(String returnNumber) {
        log.info("MerchandiseReturnServiceImpl -> findByReturnNumber: {}", returnNumber);
        MerchandiseReturn entity = returnRepository.findByReturnNumber(returnNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + returnNumber));
        return mapToDto(entity);
    }

    @Override
    public List<MerchandiseReturnDto> list(ReturnFilterDto filter) {
        log.info("MerchandiseReturnServiceImpl -> list");
        List<MerchandiseReturn> results;

        if (filter == null) {
            results = returnRepository.findAll();
            return results.stream().map(this::mapToDto).collect(Collectors.toList());
        }

        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.getReturnType() != null) {
            criteriaList.add(Criteria.where("return_type").is(filter.getReturnType()));
        }
        if (filter.getStatus() != null) {
            criteriaList.add(Criteria.where("status").is(filter.getStatus()));
        }
        if (filter.getClientId() != null && !filter.getClientId().isBlank()) {
            criteriaList.add(Criteria.where("client_id").is(filter.getClientId()));
        }
        if (filter.getSupplierId() != null && !filter.getSupplierId().isBlank()) {
            criteriaList.add(Criteria.where("supplier_id").is(filter.getSupplierId()));
        }
        if (filter.getOriginalDocumentNumber() != null && !filter.getOriginalDocumentNumber().isBlank()) {
            criteriaList.add(Criteria.where("original_document_number")
                    .regex(".*" + filter.getOriginalDocumentNumber() + ".*", "i"));
        }
        if (filter.getFromDate() != null && filter.getToDate() != null) {
            criteriaList.add(Criteria.where("return_date")
                    .gte(filter.getFromDate().atStartOfDay().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime())
                    .lte(filter.getToDate().atTime(LocalTime.MAX).atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime()));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "return_date"));

        results = mongoTemplate.find(query, MerchandiseReturn.class);
        return results.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // ========== ANULACIÓN ==========

    @Override
    @Transactional
    public MerchandiseReturnDto cancelReturn(String id, String cancelReason, String userId) {
        log.info("MerchandiseReturnServiceImpl -> cancelReturn: {}", id);

        MerchandiseReturn entity = returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + id));

        if (entity.getStatus() == EReturnStatus.ANULADA) {
            throw new SaveRecordException("La devolución ya está anulada");
        }

        // Revertir movimientos de inventario
        if (entity.getReturnType() == EReturnType.DEVOLUCION_VENTA) {
            reverseStockForSaleReturn(entity, userId);
            reverseFinancialsForSaleReturn(entity, userId);
        } else {
            reverseStockForPurchaseReturn(entity, userId);
        }

        entity.setStatus(EReturnStatus.ANULADA);
        entity.setCancelReason(cancelReason);
        entity.setCancelledAt(DateTimeUtil.nowOffsetDateTime());
        returnRepository.save(entity);

        log.info("Devolución anulada: {}", entity.getReturnNumber());
        return mapToDto(entity);
    }

    // ========== VALIDACIONES ==========

    private void validateReturnItems(List<MerchandiseReturnItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new SaveRecordException("La devolución debe tener al menos un ítem");
        }
        for (MerchandiseReturnItemDto item : items) {
            if (item.getProductId() == null && item.getProductCode() == null) {
                throw new SaveRecordException("Cada ítem debe tener productId o productCode");
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new SaveRecordException("La cantidad de cada ítem debe ser mayor a cero");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new SaveRecordException("El precio unitario no puede ser negativo");
            }
        }
    }

    private Billing findOriginalBilling(MerchandiseReturnDto dto) {
        Billing billing = null;
        if (dto.getOriginalDocumentId() != null && !dto.getOriginalDocumentId().isBlank()) {
            billing = billingRepository.findById(dto.getOriginalDocumentId()).orElse(null);
        }
        if (billing == null && dto.getOriginalDocumentNumber() != null && !dto.getOriginalDocumentNumber().isBlank()) {
            billing = billingRepository.findByBillNumber(dto.getOriginalDocumentNumber());
        }
        if (billing == null) {
            throw new ResourceNotFoundException("Factura de venta original no encontrada");
        }
        return billing;
    }

    private PurchaseInvoice findOriginalPurchaseInvoice(MerchandiseReturnDto dto) {
        PurchaseInvoice invoice = null;
        if (dto.getOriginalDocumentId() != null && !dto.getOriginalDocumentId().isBlank()) {
            invoice = purchaseInvoiceRepository.findById(dto.getOriginalDocumentId()).orElse(null);
        }
        if (invoice == null && dto.getOriginalDocumentNumber() != null && !dto.getOriginalDocumentNumber().isBlank()) {
            invoice = purchaseInvoiceRepository.findByInvoiceNumber(dto.getOriginalDocumentNumber());
        }
        if (invoice == null) {
            throw new ResourceNotFoundException("Factura de compra original no encontrada");
        }
        return invoice;
    }

    // ========== STOCK ==========

    private void updateStockForSaleReturn(List<MerchandiseReturnItemDto> items, String returnId, String userId) {
        for (MerchandiseReturnItemDto item : items) {
            String productId = item.getProductId();
            if (productId == null || productId.isBlank()) continue;

            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                log.warn("Producto no encontrado para devolución de venta: {}", productId);
                continue;
            }

            Double stockQty = calculateStockQuantityForSaleReturn(product, item.getPresentationBarcode(), item.getQuantity());
            inventoryService.registerSaleReturnMovement(returnId, productId, stockQty, item.getPresentationBarcode(), userId);
        }
    }

    private void updateStockForPurchaseReturn(List<MerchandiseReturnItemDto> items, String returnId, String userId) {
        for (MerchandiseReturnItemDto item : items) {
            String productId = item.getProductId();
            if (productId == null || productId.isBlank()) continue;

            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                log.warn("Producto no encontrado para devolución de compra: {}", productId);
                continue;
            }

            Double stockQty = calculateStockQuantityForPurchaseReturn(product, item.getPresentationBarcode(), item.getQuantity());
            inventoryService.registerPurchaseReturnMovement(returnId, productId, stockQty, item.getPresentationBarcode(), userId);
        }
    }

    private void reverseStockForSaleReturn(MerchandiseReturn entity, String userId) {
        if (entity.getItems() == null) return;
        for (MerchandiseReturnItem item : entity.getItems()) {
            if (item.getProductId() == null) continue;
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null) continue;
            Double stockQty = calculateStockQuantityForSaleReturn(product, item.getPresentationBarcode(), item.getQuantity());
            // Revertir: descontar el stock que se había incrementado (equivalente a una venta)
            inventoryService.registerSaleMovement(
                    "ANULACION-DEV-" + entity.getId(),
                    item.getProductId(),
                    stockQty,
                    item.getPresentationBarcode(),
                    userId);
        }
    }

    private void reverseStockForPurchaseReturn(MerchandiseReturn entity, String userId) {
        if (entity.getItems() == null) return;
        for (MerchandiseReturnItem item : entity.getItems()) {
            if (item.getProductId() == null) continue;
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null) continue;
            Double stockQty = calculateStockQuantityForPurchaseReturn(product, item.getPresentationBarcode(), item.getQuantity());
            // Revertir: incrementar el stock que se había decrementado (equivalente a una compra)
            inventoryService.registerPurchaseMovement(
                    "ANULACION-DEV-" + entity.getId(),
                    item.getProductId(),
                    stockQty,
                    item.getPresentationBarcode(),
                    userId);
        }
    }

    // ========== IMPLICACIONES FINANCIERAS ==========

    private void resolveSaleReturnFinancials(MerchandiseReturnDto dto, Billing billing,
                                              String returnId, String userId) {
        if (dto.getResolution() == null || dto.getTotalAmount() == null) return;

        boolean isOriginalCredit = billing.getSaleType() == EPaymentType.CREDITO;
        String clientId = dto.getClientId() != null ? dto.getClientId() :
                (billing.getClient() != null ? billing.getClient().getId() : null);
        BigDecimal totalAmount = dto.getTotalAmount();
        String returnNumber = dto.getReturnNumber();

        // Detectar si la factura original fue pagada (total o parcialmente) con saldo a favor
        BigDecimal creditUsed = BigDecimal.ZERO;
        if (clientId != null && billing.getId() != null) {
            creditUsed = clientCreditService.getCreditUsedForBilling(clientId, billing.getId());
        }
        // La porción a restaurar como crédito no puede superar el total de la devolución
        BigDecimal creditToRestore = creditUsed.min(totalAmount);
        BigDecimal cashToRefund = totalAmount.subtract(creditToRestore);

        switch (dto.getResolution()) {
            case NOTA_CREDITO:
                // Todo vuelve como saldo a favor, independientemente del método de pago original
                if (clientId != null) {
                    DepositCreditRequest depositRequest = DepositCreditRequest.builder()
                            .clientId(clientId)
                            .amount(totalAmount)
                            .paymentMethod(EPaymentMethod.SALDO_FAVOR)
                            .reference(returnNumber)
                            .notes("Saldo a favor por devolución " + returnNumber)
                            .build();
                    clientCreditService.registerDeposit(depositRequest, userId);
                    log.info("Nota crédito generada para cliente {}: ${}", clientId, totalAmount);
                }
                dto.setCreditRestored(totalAmount);
                dto.setCashRefundAmount(BigDecimal.ZERO);
                // Si la venta original era a crédito (cuenta corriente), reducir también la deuda
                if (isOriginalCredit && clientId != null) {
                    clientAccountService.reduceDebtForReturn(clientId, totalAmount, returnId,
                            "Reducción de deuda por devolución " + returnNumber, userId);
                }
                break;

            case REEMBOLSO_EFECTIVO:
            case REEMBOLSO_TRANSFERENCIA:
                // Si parte de la venta se pagó con saldo a favor, esa porción vuelve como crédito
                if (creditToRestore.compareTo(BigDecimal.ZERO) > 0 && clientId != null) {
                    DepositCreditRequest restoreRequest = DepositCreditRequest.builder()
                            .clientId(clientId)
                            .amount(creditToRestore)
                            .paymentMethod(EPaymentMethod.SALDO_FAVOR)
                            .reference(returnNumber)
                            .notes("Restauración de saldo a favor por devolución " + returnNumber)
                            .build();
                    clientCreditService.registerDeposit(restoreRequest, userId);
                    log.info("Saldo a favor restaurado para cliente {}: ${}", clientId, creditToRestore);
                }
                dto.setCreditRestored(creditToRestore);
                dto.setCashRefundAmount(cashToRefund);
                // Si la venta original era a crédito, reducir la deuda
                if (isOriginalCredit && clientId != null) {
                    clientAccountService.reduceDebtForReturn(clientId, totalAmount, returnId,
                            "Reducción de deuda por devolución (reembolso) " + returnNumber, userId);
                }
                log.info("Reembolso: crédito restaurado=${}, efectivo a entregar=${}", creditToRestore, cashToRefund);
                break;

            case CAMBIO_PRODUCTO:
                // Restaurar saldo a favor si la parte pagada con crédito aplica
                if (creditToRestore.compareTo(BigDecimal.ZERO) > 0 && clientId != null) {
                    DepositCreditRequest restoreRequest = DepositCreditRequest.builder()
                            .clientId(clientId)
                            .amount(creditToRestore)
                            .paymentMethod(EPaymentMethod.SALDO_FAVOR)
                            .reference(returnNumber)
                            .notes("Restauración de saldo a favor por cambio de producto " + returnNumber)
                            .build();
                    clientCreditService.registerDeposit(restoreRequest, userId);
                }
                dto.setCreditRestored(creditToRestore);
                dto.setCashRefundAmount(cashToRefund);
                log.info("Cambio de producto registrado: {}. Crear nueva venta para el producto de reemplazo.", returnNumber);
                break;

            default:
                break;
        }
    }

    private void reverseFinancialsForSaleReturn(MerchandiseReturn entity, String userId) {
        if (entity.getResolution() == null || entity.getTotalAmount() == null) return;
        String clientId = entity.getClientId();
        BigDecimal creditRestored = entity.getCreditRestored() != null ? entity.getCreditRestored() : BigDecimal.ZERO;

        // Revertir cualquier saldo a favor que haya sido restaurado/generado
        if (creditRestored.compareTo(BigDecimal.ZERO) > 0 && clientId != null) {
            AdjustCreditRequest adjustRequest = AdjustCreditRequest.builder()
                    .clientId(clientId)
                    .amount(creditRestored.negate())
                    .notes("Reversión de saldo a favor por anulación de devolución " + entity.getReturnNumber())
                    .build();
            clientCreditService.adjustCredit(adjustRequest, userId);
            log.info("Saldo a favor revertido al anular devolución: cliente={}, monto={}", clientId, creditRestored);
        }
    }

    // ========== CÁLCULO DE STOCK (considera fixedAmount de presentaciones) ==========

    private Double calculateStockQuantityForSaleReturn(Product product, String barcode, BigDecimal quantity) {
        if (product.getSaleType() == null || product.getSaleType() == ESale.UNIT) {
            return quantity.doubleValue();
        }
        if (barcode != null && product.getPresentations() != null) {
            for (Presentation p : product.getPresentations()) {
                if (barcode.equals(p.getBarcode())
                        && Boolean.TRUE.equals(p.getIsFixedAmount())
                        && p.getFixedAmount() != null
                        && p.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    return quantity.multiply(p.getFixedAmount()).doubleValue();
                }
            }
        }
        return quantity.doubleValue();
    }

    private Double calculateStockQuantityForPurchaseReturn(Product product, String barcode, BigDecimal quantity) {
        if (product.getSaleType() == null || product.getSaleType() == ESale.UNIT) {
            return quantity.doubleValue();
        }
        if (barcode != null && product.getPresentations() != null) {
            for (Presentation p : product.getPresentations()) {
                if (barcode.equals(p.getBarcode())
                        && p.getFixedAmount() != null
                        && p.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    return quantity.multiply(p.getFixedAmount()).doubleValue();
                }
            }
            // Fallback: primera presentación con fixedAmount
            for (Presentation p : product.getPresentations()) {
                if (p.getFixedAmount() != null && p.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    return quantity.multiply(p.getFixedAmount()).doubleValue();
                }
            }
        }
        return quantity.doubleValue();
    }

    // ========== CÁLCULO DE TOTALES ==========

    private void calculateReturnTotals(MerchandiseReturnDto dto) {
        if (dto.getItems() == null) return;

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;

        for (MerchandiseReturnItemDto item : dto.getItems()) {
            if (item.getUnitPrice() == null || item.getQuantity() == null) continue;

            BigDecimal vatRate = item.getVatRate() != null ? item.getVatRate() : BigDecimal.ZERO;
            BigDecimal itemSubtotal = item.getUnitPrice().multiply(item.getQuantity())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal vatAmount = itemSubtotal.multiply(vatRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = itemSubtotal.add(vatAmount);

            item.setVatAmount(vatAmount);
            item.setTotalAmount(itemTotal);

            subtotal = subtotal.add(itemSubtotal);
            totalVat = totalVat.add(vatAmount);
        }

        dto.setSubtotal(subtotal);
        dto.setTotalVat(totalVat);
        dto.setTotalAmount(subtotal.add(totalVat));
    }

    // ========== GENERACIÓN DE NÚMERO DE DEVOLUCIÓN ==========

    private String generateReturnNumber(EReturnType returnType) {
        String prefix = returnType == EReturnType.DEVOLUCION_VENTA ? "DEV-V" : "DEV-C";
        MerchandiseReturn last = returnRepository.findFirstByOrderByCreatedAtDesc();
        long next = 1L;
        if (last != null && last.getReturnNumber() != null) {
            try {
                String[] parts = last.getReturnNumber().split("-");
                next = Long.parseLong(parts[parts.length - 1]) + 1L;
            } catch (NumberFormatException e) {
                next = returnRepository.count() + 1L;
            }
        }
        return prefix + "-" + next;
    }

    // ========== CONSTRUCCIÓN DE ENTIDAD ==========

    private MerchandiseReturn buildReturnEntity(MerchandiseReturnDto dto, EReturnType type, String userId) {
        String returnNumber = generateReturnNumber(type);

        List<MerchandiseReturnItem> items = dto.getItems().stream()
                .map(this::mapItemToEntity)
                .collect(Collectors.toList());

        UserReference createdBy = null;
        if (dto.getCreatedBy() != null) {
            createdBy = UserReference.builder()
                    .id(dto.getCreatedBy().getId())
                    .fullName(dto.getCreatedBy().getFullName())
                    .build();
        } else if (userId != null) {
            createdBy = UserReference.builder().id(userId).build();
        }

        return MerchandiseReturn.builder()
                .returnNumber(returnNumber)
                .returnType(type)
                .originalDocumentId(dto.getOriginalDocumentId())
                .originalDocumentNumber(dto.getOriginalDocumentNumber())
                .returnDate(dto.getReturnDate() != null ? dto.getReturnDate() : DateTimeUtil.nowOffsetDateTime())
                .items(items)
                .status(EReturnStatus.PROCESADA)
                .resolution(dto.getResolution())
                .clientId(dto.getClientId())
                .clientName(dto.getClientName())
                .supplierId(dto.getSupplierId())
                .supplierName(dto.getSupplierName())
                .refundMethod(dto.getRefundMethod())
                .bankAccountId(dto.getBankAccountId())
                .bankAccountName(dto.getBankAccountName())
                .creditRestored(dto.getCreditRestored())
                .cashRefundAmount(dto.getCashRefundAmount())
                .subtotal(dto.getSubtotal())
                .totalVat(dto.getTotalVat())
                .totalAmount(dto.getTotalAmount())
                .notes(dto.getNotes())
                .createdBy(createdBy)
                .createdAt(DateTimeUtil.nowOffsetDateTime())
                .processedAt(DateTimeUtil.nowOffsetDateTime())
                .build();
    }

    private MerchandiseReturnItem mapItemToEntity(MerchandiseReturnItemDto dto) {
        return MerchandiseReturnItem.builder()
                .productId(dto.getProductId())
                .productCode(dto.getProductCode())
                .presentationBarcode(dto.getPresentationBarcode())
                .description(dto.getDescription())
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .vatRate(dto.getVatRate() != null ? dto.getVatRate() : BigDecimal.ZERO)
                .vatAmount(dto.getVatAmount() != null ? dto.getVatAmount() : BigDecimal.ZERO)
                .totalAmount(dto.getTotalAmount())
                .build();
    }

    // ========== MAPEO A DTO ==========

    private MerchandiseReturnDto mapToDto(MerchandiseReturn entity) {
        List<MerchandiseReturnItemDto> itemDtos = entity.getItems() != null
                ? entity.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList())
                : new ArrayList<>();

        UserDto createdByDto = null;
        if (entity.getCreatedBy() != null) {
            createdByDto = UserDto.builder()
                    .id(entity.getCreatedBy().getId())
                    .fullName(entity.getCreatedBy().getFullName())
                    .build();
        }

        return MerchandiseReturnDto.builder()
                .id(entity.getId())
                .returnNumber(entity.getReturnNumber())
                .returnType(entity.getReturnType())
                .originalDocumentId(entity.getOriginalDocumentId())
                .originalDocumentNumber(entity.getOriginalDocumentNumber())
                .returnDate(entity.getReturnDate())
                .items(itemDtos)
                .status(entity.getStatus())
                .resolution(entity.getResolution())
                .clientId(entity.getClientId())
                .clientName(entity.getClientName())
                .supplierId(entity.getSupplierId())
                .supplierName(entity.getSupplierName())
                .refundMethod(entity.getRefundMethod())
                .bankAccountId(entity.getBankAccountId())
                .bankAccountName(entity.getBankAccountName())
                .creditRestored(entity.getCreditRestored())
                .cashRefundAmount(entity.getCashRefundAmount())
                .subtotal(entity.getSubtotal())
                .totalVat(entity.getTotalVat())
                .totalAmount(entity.getTotalAmount())
                .notes(entity.getNotes())
                .cancelReason(entity.getCancelReason())
                .createdBy(createdByDto)
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getProcessedAt())
                .cancelledAt(entity.getCancelledAt())
                .build();
    }

    private MerchandiseReturnItemDto mapItemToDto(MerchandiseReturnItem item) {
        return MerchandiseReturnItemDto.builder()
                .productId(item.getProductId())
                .productCode(item.getProductCode())
                .presentationBarcode(item.getPresentationBarcode())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .vatRate(item.getVatRate())
                .vatAmount(item.getVatAmount())
                .totalAmount(item.getTotalAmount())
                .build();
    }
}
