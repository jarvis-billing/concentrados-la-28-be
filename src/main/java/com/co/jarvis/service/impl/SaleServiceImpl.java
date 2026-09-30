package com.co.jarvis.service.impl;

import com.co.jarvis.dto.*;
import com.co.jarvis.dto.api.model.OrderApi;
import com.co.jarvis.dto.api.model.ProductApi;
import com.co.jarvis.entity.Billing;
import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.enums.EPaymentType;
import com.co.jarvis.enums.EStatus;
import com.co.jarvis.enums.EStatusOrder;
import com.co.jarvis.enums.EVat;
import com.co.jarvis.repository.BillingRepository;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.dto.AdjustCreditRequest;
import com.co.jarvis.dto.UseCreditRequest;
import com.co.jarvis.service.*;
import com.co.jarvis.entity.Product;
import com.co.jarvis.entity.Presentation;
import com.co.jarvis.enums.ESale;
import com.co.jarvis.dto.batch.BatchSaleRequest;
import com.co.jarvis.util.DateTimeUtil;
import com.co.jarvis.util.constants.BatchConstants;
import com.co.jarvis.util.exception.*;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import com.co.jarvis.util.reports.ReportExporter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.lang.NumberFormatException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

@Slf4j
@Service
public class SaleServiceImpl implements SaleService {

    private static final Logger logger = LoggerFactory.getLogger(SaleServiceImpl.class);
    public static final String REPORT_TICKET_BILLING = "ticket_billing";

    @Autowired
    private OrderService orderService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private BillingRepository repository;

    @Autowired
    private ProductVatTypeService productVatTypeService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ReportExporter reportExporter;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private com.co.jarvis.repository.PreSaleRepository preSaleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ClientAccountService clientAccountService;

    @Autowired
    private ClientCreditService clientCreditService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private com.co.jarvis.util.BankAccountHelper bankAccountHelper;

    GenericMapper<Billing, BillingDto> mapper
            = new GenericMapper<>(Billing.class, BillingDto.class);

    @Override
    public BillingDto findByBillingNumber(String billingNumber) {
        logger.info("SaleServiceImpl -> findByBillingNumber");
        Billing billing = repository.findByBillNumber(billingNumber);
        if (billing == null) {
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        }
        return mapper.mapToDto(billing);
    }

    @Override
    public List<BillingDto> findAll() {
        log.info("SaleServiceImpl -> findAll");
        return mapper.mapToDtoList(repository.findAll());
    }

    @Override
    public BillingDto findById(String id) {
        log.info("SaleServiceImpl -> findById");
        return mapper.mapToDto(repository.findById(id).orElse(new Billing()));
    }

    @Override
    public BillingDto save(BillingDto dto) {
        log.info("SaleServiceImpl -> save");
        try {
            Billing billing = repository.findByBillNumber(dto.getBillNumber());
            if (billing != null) {
                throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR);
            }

            // Mixed payments handling
            if (dto.getSaleType() == EPaymentType.CREDITO) {
                dto.setReceivedValue(BigDecimal.ZERO);
                dto.setReturnedValue(BigDecimal.ZERO);
                // Ignore/clear methods if any provided together with payments
                if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
                    dto.setPaymentMethods(new ArrayList<>());
                }
            } else if (dto.getSaleType() == EPaymentType.CONTADO && dto.getPayments() != null && !dto.getPayments().isEmpty()) {
                // Validate and compute
                List<PaymentEntryDto> pays = dto.getPayments();
                BigDecimal total = dto.getTotalBilling() != null ? dto.getTotalBilling() : BigDecimal.ZERO;
                BigDecimal received = BigDecimal.ZERO;
                BigDecimal cash = BigDecimal.ZERO;
                BigDecimal nonCash = BigDecimal.ZERO;
                Set<EPaymentMethod> methods = new HashSet<>();

                for (PaymentEntryDto p : pays) {
                    if (p.getAmount() == null || p.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new FieldsException("Validation failed", java.util.Map.of("payments", "Monto de pago debe ser mayor a 0"));
                    }
                    if (p.getMethod() == null || p.getMethod().isBlank()) {
                        throw new FieldsException("Validation failed", java.util.Map.of("payments", "Método de pago requerido"));
                    }
                    EPaymentMethod m;
                    try {
                        m = EPaymentMethod.valueOf(p.getMethod().trim().toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        throw new FieldsException("Validation failed", java.util.Map.of("payments", "Método de pago inválido: " + p.getMethod()));
                    }
                    // Si es TRANSFERENCIA, requerir bankAccountId
                    if (m == EPaymentMethod.TRANSFERENCIA && (p.getBankAccountId() == null || p.getBankAccountId().isBlank())) {
                        throw new FieldsException("Validation failed", java.util.Map.of("payments", "bankAccountId requerido para pagos por transferencia"));
                    }
                    // Enriquecer nombre de cuenta bancaria si solo viene el ID
                    p.setBankAccountName(bankAccountHelper.resolveBankAccountName(
                            p.getBankAccountId(), p.getBankAccountName()));
                    methods.add(m);
                    received = received.add(p.getAmount());
                    if (m == EPaymentMethod.EFECTIVO) {
                        cash = cash.add(p.getAmount());
                    } else {
                        nonCash = nonCash.add(p.getAmount());
                    }
                }

                if (received.compareTo(total) < 0) {
                    throw new FieldsException("Validation failed", java.util.Map.of("payments", "Pagos insuficientes para total de contado"));
                }

                BigDecimal remaining = total.subtract(nonCash);
                if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                    remaining = BigDecimal.ZERO;
                }
                BigDecimal returned = cash.subtract(remaining);
                if (returned.compareTo(BigDecimal.ZERO) < 0) {
                    returned = BigDecimal.ZERO;
                }

                dto.setPaymentMethods(new ArrayList<>(methods));
                dto.setReceivedValue(received);
                dto.setReturnedValue(returned);
            }

            // Pre-validar lotes ANTES de guardar la factura
            validateBatchesForSale(dto.getSaleDetails());

            Billing venta = mapper.mapToEntity(dto);
            Long orderNumber = dto.getOrder().getOrderNumber();
            if (orderNumber != null && orderNumber.intValue() > 0) {
                orderService.changeStatus(orderNumber, EStatusOrder.FACTURADO);
            }

            // Guardar la factura
            Billing savedBilling = repository.save(venta);
            
            // Descontar stock de los productos vendidos
            updateStockForSale(dto.getSaleDetails(), savedBilling.getId(), 
                    dto.getCreationUser() != null ? dto.getCreationUser().getId() : null);

            // Si es venta a CRÉDITO, actualizar cuenta por cobrar del cliente
            if (dto.getSaleType() == EPaymentType.CREDITO && dto.getClient() != null && dto.getClient().getId() != null) {
                clientAccountService.addDebt(dto.getClient().getId(), dto.getTotalBilling());
                logger.info("Deuda agregada a cuenta del cliente: {} por monto: {}", 
                        dto.getClient().getId(), dto.getTotalBilling());
            }

            // Si se aplica saldo a favor del cliente
            if (dto.getCreditToApply() != null && dto.getCreditToApply().compareTo(BigDecimal.ZERO) > 0 
                    && dto.getClient() != null && dto.getClient().getId() != null) {
                UseCreditRequest useCreditRequest = UseCreditRequest.builder()
                        .clientId(dto.getClient().getId())
                        .amount(dto.getCreditToApply())
                        .billingId(savedBilling.getId())
                        .notes("Aplicado en factura " + savedBilling.getBillNumber())
                        .build();
                clientCreditService.useCredit(useCreditRequest, 
                        dto.getCreationUser() != null ? dto.getCreationUser().getId() : null);
                logger.info("Saldo a favor aplicado para cliente: {} por monto: {} en factura: {}", 
                        dto.getClient().getId(), dto.getCreditToApply(), savedBilling.getBillNumber());
            }

            return mapper.mapToDto(savedBilling);
        } catch (DuplicateRecordException e) {
            logger.error("SaleServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR, e.getCause());
        }
    }

    @Override
    public void deleteById(String id) {
        log.info("SaleServiceImpl -> deleteById");
        repository.deleteById(id);
    }

    @Override
    public BillingDto update(BillingDto dto, String id) {
        log.info("SaleServiceImpl -> update");
        try {
            Billing billing = repository.findByBillNumber(dto.getBillNumber());
            if (billing == null) {
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            dto.setId(id);
            Billing entity = mapper.mapToEntity(dto);
            return mapper.mapToDto(repository.save(entity));
        } catch (ResourceNotFoundException e) {
            logger.error("SaleServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e.getCause());
        }
    }

    @Override
    public BillingDto importSaleOrder(Long orderNumber) throws FieldsException {
        logger.info("SaleServiceImpl -> importSaleOrder");
        try {
            if (orderNumber == null || orderNumber < 0) {
                throw new FieldsException(MessageConstants.EMPTY_FIELDS, List.of("Número Orden"));
            }

            OrderDto orderDto = orderService.findByOrderNumber(orderNumber);
            if (orderDto == null || orderDto.getProducts().isEmpty()) {
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            validateStatus(orderDto);
            return buildBilling(orderDto);
        } catch (IllegalStateException e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new IllegalStateException(e.getMessage());
        } catch (FieldsException e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new FieldsException(e.getMessage());
        } catch (ResourceNotFoundException e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new ResourceNotFoundException(e.getMessage());
        } catch (ResourceEndException e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new ResourceEndException(e.getMessage());
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> importSaleOrder -> Error {}", e.getMessage());
            throw new GenericInternalException(e.getMessage());
        }
    }

    @Override
    public String getLastBillingNumber() {
        return generatedBillingNumber();
    }

    @Override
    public byte[] printTicketBilling(BillingDto dto) {
        logger.info("SaleServiceImpl -> printTicketBilling");
        try {
            Billing billing = mapper.mapToEntity(dto);
            return reportExporter.exportToPdf(billing, REPORT_TICKET_BILLING);
        } catch (JRException | FileNotFoundException e) {
            log.error(e.getCause().getMessage());
        }
        return null;
    }

    @Override
    public List<BillingDto> findAllBilling(BillingReportFilterDto dto) {
        try {
            logger.info("SaleServiceImpl -> findAllBilling");
            // Crear una lista de criterios para los filtros dinámicos
            List<Criteria> criteriaList = new ArrayList<>();

            // Filtro por rango de fechas
            if (dto.hasFilterDate()) {
                java.time.LocalDate startDate = dto.getFromDate().isBefore(dto.getToDate()) ? dto.getFromDate() : dto.getToDate();
                java.time.LocalDate endDate = dto.getFromDate().isAfter(dto.getToDate()) ? dto.getFromDate() : dto.getToDate();
                criteriaList.add(Criteria.where("dateTimeRecord")
                        .gte(startDate.atStartOfDay().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime())
                        .lte(endDate.atTime(LocalTime.MAX).atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime()));
            }

            // Filtro por numero de factura
            if (dto.getBillNumber() != null && !dto.getBillNumber().isEmpty()) {
                criteriaList.add(Criteria.where("billNumber").regex(".*" + dto.getBillNumber() + ".*", "i"));
            }

            // Filtro por usuario de venta
            if (dto.getUserSale() != null && !dto.getUserSale().isEmpty()) {
                criteriaList.add(Criteria.where("order.creationUser.numberIdentity").is(dto.getUserSale()));
            }

            // Filtro por cliente
            if (dto.getClient() != null && !dto.getClient().isEmpty()) {
                String clientField = (dto.getClientField() != null && !dto.getClientField().isEmpty())
                        ? "client." + dto.getClientField()
                        : "client._id";
                criteriaList.add(Criteria.where(clientField).is(dto.getClient()));
            }

            // Filtro por producto
            if (dto.getProduct() != null && !dto.getProduct().isEmpty()) {
                String productField = (dto.getProductField() != null && !dto.getProductField().isEmpty())
                        ? "saleDetails.product." + dto.getProductField()
                        : "saleDetails.product._id";
                criteriaList.add(Criteria.where(productField).is(dto.getProduct()));
            }

            // Filtro por tipo de venta
            if (dto.getSaleType() != null && !dto.getSaleType().isEmpty()) {
                criteriaList.add(Criteria.where("saleType").is(dto.getSaleType()));
            }

            // Filtro por método de pago
            if (dto.getPaymentMethod() != null && !dto.getPaymentMethod().isEmpty()) {
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("payments.method").is(dto.getPaymentMethod()),
                        Criteria.where("paymentMethods").is(dto.getPaymentMethod())));
            }

            // Construir el criterio final combinando todas las condiciones con AND
            Criteria finalCriteria = new Criteria();
            if (!criteriaList.isEmpty()) {
                finalCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
            }

            // Crear la consulta utilizando el criterio
            Query query = new Query(finalCriteria);
            query.with(Sort.by(Sort.Direction.DESC, "dateTimeRecord"));

            // Ejecutar la consulta y mapear los resultados a BillingDto
            List<Billing> results = mongoTemplate.find(query, Billing.class);
            List<BillingDto> dtos = mapper.mapToDtoList(results);

            // Enriquecer con info de preventa — cruzamos PreSale.billingId → BillingDto
            enrichWithPreSaleInfo(dtos, dto.getHasPreSale());

            return dtos;
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> findAllBilling -> Error {}", e.getMessage());
            throw new GenericInternalException(e.getMessage());
        }
    }

    /**
     * Enriquece cada BillingDto con la información de sus preventas:
     * - preSaleIds: lista de IDs de preventas que componen la factura
     * - preSaleNumber: números legibles concatenados (ej: "PRV-0001, PRV-0002")
     *
     * Estrategia:
     * 1. Si el Billing ya tiene preSaleIds guardados → usarlos directamente (datos nuevos)
     * 2. Si no → cross-reference desde PreSale.billingId (retrocompatibilidad con datos históricos)
     */
    private void enrichWithPreSaleInfo(List<BillingDto> dtos, Boolean hasPreSaleFilter) {
        // Cargar todas las preventas facturadas para cross-reference histórico
        List<com.co.jarvis.entity.PreSale> billedPreSales =
                preSaleRepository.findByBillingIdNotNull();

        // Mapa: billingId → lista de PreSales (una factura puede tener varias preventas)
        java.util.Map<String, List<com.co.jarvis.entity.PreSale>> billingToPreSales =
                billedPreSales.stream()
                        .filter(ps -> ps.getBillingId() != null)
                        .collect(java.util.stream.Collectors.groupingBy(
                                com.co.jarvis.entity.PreSale::getBillingId));

        // Mapa: preSaleId → PreSale (para lookup por ID directo)
        java.util.Map<String, com.co.jarvis.entity.PreSale> preSaleById =
                billedPreSales.stream()
                        .filter(ps -> ps.getId() != null)
                        .collect(java.util.stream.Collectors.toMap(
                                com.co.jarvis.entity.PreSale::getId,
                                ps -> ps,
                                (a, b) -> a));

        dtos.forEach(dto -> {
            List<com.co.jarvis.entity.PreSale> related;

            if (dto.getPreSaleIds() != null && !dto.getPreSaleIds().isEmpty()) {
                // Factura nueva: tiene los IDs guardados directamente
                related = dto.getPreSaleIds().stream()
                        .map(preSaleById::get)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
            } else {
                // Factura histórica: buscar por cross-reference
                related = billingToPreSales.getOrDefault(dto.getId(), java.util.List.of());
                // Guardar los IDs para futuras consultas (migración implícita)
                if (!related.isEmpty()) {
                    dto.setPreSaleIds(related.stream()
                            .map(com.co.jarvis.entity.PreSale::getId)
                            .collect(java.util.stream.Collectors.toList()));
                }
            }

            if (!related.isEmpty()) {
                // Construir string legible con todos los números de preventa
                String numbers = related.stream()
                        .map(com.co.jarvis.entity.PreSale::getPreSaleNumber)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.joining(", "));
                dto.setPreSaleNumber(numbers.isEmpty() ? null : numbers);
            }
        });

        // Aplicar filtro si viene especificado
        if (hasPreSaleFilter != null) {
            dtos.removeIf(dto -> hasPreSaleFilter
                    ? dto.getPreSaleNumber() == null        // true → solo con preventa
                    : dto.getPreSaleNumber() != null);      // false → solo sin preventa
        }
    }

    @Override
    public Page<BillingDto> findAllBillingPaged(BillingReportFilterPagedDto dto) {
        try {
            logger.info("SaleServiceImpl -> findAllBillingPaged: page={}, size={}", dto.getPage(), dto.getSize());
            List<Criteria> criteriaList = buildCriteriaFromPagedFilter(dto);

            Criteria finalCriteria = criteriaList.isEmpty()
                    ? new Criteria()
                    : new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));

            Pageable pageable = PageRequest.of(dto.getPage(), dto.getSize(),
                    Sort.by(Sort.Direction.DESC, "dateTimeRecord"));

            Query countQuery = new Query(finalCriteria);
            long total = mongoTemplate.count(countQuery, Billing.class);

            Query query = new Query(finalCriteria)
                    .with(pageable);
            List<Billing> results = mongoTemplate.find(query, Billing.class);

            List<BillingDto> dtos = mapper.mapToDtoList(results);
            return new PageImpl<>(dtos, pageable, total);
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> findAllBillingPaged -> Error {}", e.getMessage());
            throw new GenericInternalException(e.getMessage());
        }
    }

    @Override
    public SalesTotalsResponse getSalesTotals(BillingReportFilterDto dto) {
        try {
            logger.info("SaleServiceImpl -> getSalesTotals");
            List<Criteria> criteriaList = new ArrayList<>();

            if (dto.hasFilterDate()) {
                java.time.LocalDate startDate = dto.getFromDate().isBefore(dto.getToDate()) ? dto.getFromDate() : dto.getToDate();
                java.time.LocalDate endDate = dto.getFromDate().isAfter(dto.getToDate()) ? dto.getFromDate() : dto.getToDate();
                criteriaList.add(Criteria.where("dateTimeRecord")
                        .gte(startDate.atStartOfDay().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime())
                        .lte(endDate.atTime(LocalTime.MAX).atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime()));
            }
            if (dto.getBillNumber() != null && !dto.getBillNumber().isEmpty()) {
                criteriaList.add(Criteria.where("billNumber").regex(".*" + dto.getBillNumber() + ".*", "i"));
            }
            if (dto.getUserSale() != null && !dto.getUserSale().isEmpty()) {
                criteriaList.add(Criteria.where("order.creationUser.numberIdentity").is(dto.getUserSale()));
            }
            if (dto.getClient() != null && !dto.getClient().isEmpty()) {
                String clientField = (dto.getClientField() != null && !dto.getClientField().isEmpty())
                        ? "client." + dto.getClientField()
                        : "client._id";
                criteriaList.add(Criteria.where(clientField).is(dto.getClient()));
            }
            if (dto.getProduct() != null && !dto.getProduct().isEmpty()) {
                String productField = (dto.getProductField() != null && !dto.getProductField().isEmpty())
                        ? "saleDetails.product." + dto.getProductField()
                        : "saleDetails.product._id";
                criteriaList.add(Criteria.where(productField).is(dto.getProduct()));
            }
            if (dto.getSaleType() != null && !dto.getSaleType().isEmpty()) {
                criteriaList.add(Criteria.where("saleType").is(dto.getSaleType()));
            }
            if (dto.getPaymentMethod() != null && !dto.getPaymentMethod().isEmpty()) {
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("payments.method").is(dto.getPaymentMethod()),
                        Criteria.where("paymentMethods").is(dto.getPaymentMethod())));
            }

            Criteria finalCriteria = criteriaList.isEmpty()
                    ? new Criteria()
                    : new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));

            Query query = new Query(finalCriteria);
            List<Billing> billings = mongoTemplate.find(query, Billing.class);

            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalIva = BigDecimal.ZERO;
            BigDecimal totalGeneral = BigDecimal.ZERO;
            BigDecimal totalContado = BigDecimal.ZERO;
            BigDecimal totalCredito = BigDecimal.ZERO;
            long countContado = 0;
            long countCredito = 0;

            java.util.Map<String, long[]> methodMap = new java.util.LinkedHashMap<>();

            for (Billing b : billings) {
                BigDecimal total = b.getTotalBilling() != null ? b.getTotalBilling() : BigDecimal.ZERO;
                BigDecimal subtotal = b.getSubTotalSale() != null ? b.getSubTotalSale() : BigDecimal.ZERO;
                BigDecimal iva = b.getTotalIVAT() != null ? b.getTotalIVAT() : BigDecimal.ZERO;

                totalSubtotal = totalSubtotal.add(subtotal);
                totalIva = totalIva.add(iva);
                totalGeneral = totalGeneral.add(total);

                if (b.getSaleType() == EPaymentType.CONTADO) {
                    countContado++;
                    totalContado = totalContado.add(total);
                } else {
                    countCredito++;
                    totalCredito = totalCredito.add(total);
                }

                if (b.getPayments() != null && !b.getPayments().isEmpty()) {
                    for (com.co.jarvis.entity.PaymentEntry p : b.getPayments()) {
                        String method = p.getMethod() != null ? p.getMethod() : "EFECTIVO";
                        methodMap.computeIfAbsent(method, k -> new long[2]);
                        methodMap.get(method)[0]++;
                        methodMap.get(method)[1] += p.getAmount() != null ? p.getAmount().longValue() : 0;
                    }
                } else if (b.getPaymentMethods() != null && !b.getPaymentMethods().isEmpty()) {
                    String method = b.getPaymentMethods().get(0).name();
                    methodMap.computeIfAbsent(method, k -> new long[2]);
                    methodMap.get(method)[0]++;
                    methodMap.get(method)[1] += total.longValue();
                }
            }

            List<SalesTotalsResponse.PaymentMethodTotalDto> paymentMethodTotals = methodMap.entrySet().stream()
                    .map(e -> SalesTotalsResponse.PaymentMethodTotalDto.builder()
                            .method(e.getKey())
                            .count(e.getValue()[0])
                            .total(BigDecimal.valueOf(e.getValue()[1]))
                            .build())
                    .collect(java.util.stream.Collectors.toList());

            return SalesTotalsResponse.builder()
                    .totalInvoices(billings.size())
                    .totalSubtotal(totalSubtotal)
                    .totalIva(totalIva)
                    .totalGeneral(totalGeneral)
                    .countContado(countContado)
                    .countCredito(countCredito)
                    .totalContado(totalContado)
                    .totalCredito(totalCredito)
                    .paymentMethodTotals(paymentMethodTotals)
                    .build();
        } catch (Exception e) {
            logger.error("SaleServiceImpl -> getSalesTotals -> Error {}", e.getMessage());
            throw new GenericInternalException(e.getMessage());
        }
    }

    private List<Criteria> buildCriteriaFromPagedFilter(BillingReportFilterPagedDto dto) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Rango de fechas — default últimos 30 días si no se especifica
        if (dto.getFromDate() != null && !dto.getFromDate().isBlank()
                && dto.getToDate() != null && !dto.getToDate().isBlank()) {
            java.time.LocalDate parsed1 = java.time.LocalDate.parse(dto.getFromDate());
            java.time.LocalDate parsed2 = java.time.LocalDate.parse(dto.getToDate());
            java.time.LocalDate startDate = parsed1.isBefore(parsed2) ? parsed1 : parsed2;
            java.time.LocalDate endDate = parsed1.isAfter(parsed2) ? parsed1 : parsed2;
            criteriaList.add(Criteria.where("dateTimeRecord")
                    .gte(startDate.atStartOfDay().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime())
                    .lte(endDate.atTime(LocalTime.MAX).atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime()));
        } else {
            java.time.LocalDate today = java.time.LocalDate.now(DateTimeUtil.getBogotaZone());
            java.time.LocalDate thirtyDaysAgo = today.minusDays(30);
            criteriaList.add(Criteria.where("dateTimeRecord")
                    .gte(thirtyDaysAgo.atStartOfDay().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime())
                    .lte(today.atTime(LocalTime.MAX).atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime()));
        }

        if (dto.getBillNumber() != null && !dto.getBillNumber().isBlank()) {
            criteriaList.add(Criteria.where("billNumber").regex(".*" + dto.getBillNumber() + ".*", "i"));
        }
        if (dto.getUserSale() != null && !dto.getUserSale().isBlank()) {
            criteriaList.add(Criteria.where("order.creationUser.numberIdentity").is(dto.getUserSale()));
        }
        if (dto.getClient() != null && !dto.getClient().isBlank()) {
            criteriaList.add(Criteria.where("client._id").is(dto.getClient()));
        }
        if (dto.getProduct() != null && !dto.getProduct().isBlank()) {
            criteriaList.add(Criteria.where("saleDetails.product._id").is(dto.getProduct()));
        }
        if (dto.getSaleType() != null && !dto.getSaleType().isBlank()) {
            criteriaList.add(Criteria.where("saleType").is(dto.getSaleType()));
        }
        if (dto.getPaymentMethod() != null && !dto.getPaymentMethod().isBlank()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("payments.method").is(dto.getPaymentMethod()),
                    Criteria.where("paymentMethods").is(dto.getPaymentMethod())));
        }

        return criteriaList;
    }

    @Override
    public BillingDto updateBilling(String id, BillingDto dto) {
        log.info("SaleServiceImpl -> updateBilling: id={}", id);
        Billing existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada: " + id));

        String createdBy = existing.getCreationUser() != null ? existing.getCreationUser().getId() : "system";

        // ── Estado anterior ──────────────────────────────────────────────────────
        String oldClientId = existing.getClient() != null ? existing.getClient().getId() : null;
        EPaymentType oldSaleType = existing.getSaleType();
        BigDecimal totalBilling = existing.getTotalBilling() != null ? existing.getTotalBilling() : BigDecimal.ZERO;

        // Saldo a favor original usado en esta factura
        BigDecimal oldCreditUsed = BigDecimal.ZERO;
        if (oldClientId != null) {
            oldCreditUsed = clientCreditService.getCreditUsedForBilling(oldClientId, id);
        }

        // ── Estado nuevo ─────────────────────────────────────────────────────────
        String newClientId = (dto.getClient() != null && dto.getClient().getId() != null)
                ? dto.getClient().getId()
                : oldClientId;
        EPaymentType newSaleType = dto.getSaleType() != null ? dto.getSaleType() : oldSaleType;

        // Saldo a favor nuevo (suma de filas con método SALDO_FAVOR en los pagos del dto)
        BigDecimal newCreditUsed = BigDecimal.ZERO;
        if (dto.getPayments() != null) {
            newCreditUsed = dto.getPayments().stream()
                    .filter(p -> EPaymentMethod.SALDO_FAVOR.name().equals(p.getMethod()))
                    .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        boolean clientChanged = oldClientId != null && !oldClientId.equals(newClientId);

        // ══════════════════════════════════════════════════════════════════════════
        // RECONCILIACIÓN DE SALDO A FAVOR
        // ══════════════════════════════════════════════════════════════════════════

        if (clientChanged) {
            // El cliente cambió: restaurar crédito al cliente original y descontar del nuevo
            if (oldCreditUsed.compareTo(BigDecimal.ZERO) > 0) {
                // Devolver saldo al cliente anterior
                AdjustCreditRequest restore = AdjustCreditRequest.builder()
                        .clientId(oldClientId)
                        .amount(oldCreditUsed)
                        .notes("Reversión de saldo a favor por edición de factura " + existing.getBillNumber())
                        .build();
                clientCreditService.adjustCredit(restore, createdBy);
                log.info("Saldo a favor restaurado al cliente anterior {}: {}", oldClientId, oldCreditUsed);
            }
            if (newCreditUsed.compareTo(BigDecimal.ZERO) > 0) {
                // Validar y descontar del nuevo cliente
                BigDecimal availableBalance = clientCreditService.getClientCreditBalance(newClientId);
                if (newCreditUsed.compareTo(availableBalance) > 0) {
                    throw new RuntimeException(
                            String.format("El nuevo cliente no tiene saldo suficiente para aplicar saldo a favor. " +
                                    "Saldo disponible: %s, Monto requerido: %s", availableBalance, newCreditUsed));
                }
                UseCreditRequest useReq = UseCreditRequest.builder()
                        .clientId(newClientId)
                        .amount(newCreditUsed)
                        .billingId(id)
                        .notes("Saldo a favor aplicado por reasignación de factura " + existing.getBillNumber())
                        .build();
                clientCreditService.useCredit(useReq, createdBy);
                log.info("Saldo a favor descontado al nuevo cliente {}: {}", newClientId, newCreditUsed);
            }
        } else {
            // Mismo cliente — reconciliar la diferencia
            BigDecimal diff = newCreditUsed.subtract(oldCreditUsed);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                // Se usa MÁS saldo a favor → descontar la diferencia
                BigDecimal availableBalance = clientCreditService.getClientCreditBalance(newClientId);
                if (diff.compareTo(availableBalance) > 0) {
                    throw new RuntimeException(
                            String.format("El cliente no tiene saldo suficiente para aplicar el monto adicional. " +
                                    "Saldo disponible: %s, Monto adicional requerido: %s", availableBalance, diff));
                }
                UseCreditRequest useReq = UseCreditRequest.builder()
                        .clientId(newClientId)
                        .amount(diff)
                        .billingId(id)
                        .notes("Ajuste de saldo a favor en edición de factura " + existing.getBillNumber())
                        .build();
                clientCreditService.useCredit(useReq, createdBy);
                log.info("Saldo a favor adicional descontado al cliente {}: {}", newClientId, diff);
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // Se usa MENOS saldo a favor → devolver la diferencia
                BigDecimal toRestore = diff.abs();
                AdjustCreditRequest restore = AdjustCreditRequest.builder()
                        .clientId(newClientId)
                        .amount(toRestore)
                        .notes("Devolución parcial de saldo a favor en edición de factura " + existing.getBillNumber())
                        .build();
                clientCreditService.adjustCredit(restore, createdBy);
                log.info("Saldo a favor parcialmente restaurado al cliente {}: {}", newClientId, toRestore);
            }
            // Si diff == 0 no hay nada que hacer en créditos
        }

        // ══════════════════════════════════════════════════════════════════════════
        // RECONCILIACIÓN DE TIPO DE VENTA (CONTADO / CRÉDITO)
        // ══════════════════════════════════════════════════════════════════════════

        boolean saleTypeChanged = !oldSaleType.equals(newSaleType);

        if (clientChanged && oldSaleType == EPaymentType.CREDITO) {
            // Remover deuda del cliente anterior independientemente del nuevo tipo
            clientAccountService.reduceDebtForReturn(
                    oldClientId,
                    totalBilling,
                    null,
                    "Deuda removida por reasignación de factura " + existing.getBillNumber(),
                    createdBy);
            log.info("Deuda removida del cliente anterior {}: {}", oldClientId, totalBilling);
        }

        if (clientChanged && newSaleType == EPaymentType.CREDITO) {
            // Agregar deuda al nuevo cliente
            clientAccountService.addDebt(newClientId, totalBilling);
            log.info("Deuda agregada al nuevo cliente {}: {}", newClientId, totalBilling);
        }

        if (!clientChanged && saleTypeChanged) {
            if (oldSaleType == EPaymentType.CONTADO && newSaleType == EPaymentType.CREDITO) {
                // CONTADO → CRÉDITO: crear deuda
                clientAccountService.addDebt(newClientId, totalBilling);
                log.info("Venta cambiada a CRÉDITO. Deuda agregada al cliente {}: {}", newClientId, totalBilling);
            } else if (oldSaleType == EPaymentType.CREDITO && newSaleType == EPaymentType.CONTADO) {
                // CRÉDITO → CONTADO: eliminar deuda
                clientAccountService.reduceDebtForReturn(
                        oldClientId,
                        totalBilling,
                        null,
                        "Deuda removida por cambio a venta de contado en factura " + existing.getBillNumber(),
                        createdBy);
                log.info("Venta cambiada a CONTADO. Deuda eliminada del cliente {}: {}", oldClientId, totalBilling);
            }
        }

        // ══════════════════════════════════════════════════════════════════════════
        // ACTUALIZAR CAMPOS DE LA FACTURA
        // ══════════════════════════════════════════════════════════════════════════

        if (dto.getDateTimeRecord() != null) {
            existing.setDateTimeRecord(dto.getDateTimeRecord());
        }
        if (dto.getClient() != null && dto.getClient().getId() != null) {
            com.co.jarvis.entity.Client c = new com.co.jarvis.entity.Client();
            c.setId(dto.getClient().getId());
            c.setName(dto.getClient().getName());
            c.setSurname(dto.getClient().getSurname());
            c.setIdNumber(dto.getClient().getIdNumber());
            c.setFullName(dto.getClient().getFullName());
            c.setEmail(dto.getClient().getEmail());
            c.setPhone(dto.getClient().getPhone());
            existing.setClient(c);
        }
        if (dto.getPaymentMethods() != null) {
            existing.setPaymentMethods(dto.getPaymentMethods());
        }
        existing.setSaleType(newSaleType);
        if (dto.getBillingType() != null) {
            existing.setBillingType(dto.getBillingType());
        }
        if (dto.getIsReportInvoice() != null) {
            existing.setIsReportInvoice(dto.getIsReportInvoice());
        }
        if (dto.getReceivedValue() != null) {
            existing.setReceivedValue(dto.getReceivedValue());
        }
        if (dto.getReturnedValue() != null) {
            existing.setReturnedValue(dto.getReturnedValue());
        }
        if (dto.getPayments() != null) {
            existing.setPayments(dto.getPayments().stream()
                    .map(p -> {
                        com.co.jarvis.entity.PaymentEntry entry = new com.co.jarvis.entity.PaymentEntry();
                        entry.setMethod(p.getMethod());
                        entry.setAmount(p.getAmount());
                        entry.setReference(p.getReference());
                        entry.setBankAccountId(p.getBankAccountId());
                        entry.setBankAccountName(p.getBankAccountName());
                        return entry;
                    })
                    .collect(java.util.stream.Collectors.toList()));
        }

        return mapper.mapToDto(repository.save(existing));
    }

    @Override
    public List<ProductSalesSummary> getProductSalesSummary(BillingReportFilterDto dto) {
        if (dto.hasFilterDate()) {
            return repository.getProductSalesSummaryByDate(
                    dto.getFromDate().atStartOfDay().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime(), 
                    dto.getToDate().atTime(LocalTime.now()).atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime());
        }

        throw new RuntimeException("not filter data for products summary");
    }

    private static void validateStatus(OrderDto orderDto) {
        if (orderDto.getStatus() == null || orderDto.getStatus().equals(EStatusOrder.INICIADO)) {
            throw new ResourceEndException(format(MessageConstants.ORDER_NOT_FINISHED, orderDto.getOrderNumber(),
                    orderDto.getCreationUser().getFullName()));
        }
    }

    private BillingDto buildBilling(OrderDto orderDto) {
        List<SaleDetailDto> saleDetailDtos = getSaleDetail(orderDto);
        return BillingDto.builder()
                .billNumber(generatedBillingNumber())
                .client(getClientOrder(orderDto))
                .order(OrderApi.builder()
                        .id(orderDto.getId())
                        .orderNumber(orderDto.getOrderNumber())
                        .creationDate(orderDto.getCreationDate())
                        .creationUser(orderDto.getCreationUser())
                        .build())
                .dateTimeRecord(orderDto.getCreationDate() != null 
                    ? orderDto.getCreationDate().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime()
                    : DateTimeUtil.nowOffsetDateTime())
                .saleDetails(saleDetailDtos)
                .subTotalSale(subTotalBill(saleDetailDtos))
                .totalIVAT(ivaTotalBill(saleDetailDtos))
                .company(companyService.findByStatus(EStatus.ACTIVO))
                .build();
    }

    private ClientDto getClientOrder(OrderDto order) {
        ClientDto clientDefault = clientService.getClientDefault();
        return order.getClient().getIdNumber().isBlank() ? clientDefault : order.getClient();
    }

    private String generatedBillingNumber() {
        CompanyDto companyDto = companyService.findByStatus(EStatus.ACTIVO);
        if (companyDto == null) {
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND + " - Empresa no Encontrada");
        }

        BillingConfigDto billingConfigDto = Optional.ofNullable(companyDto.getBillingConfig())
                .orElseThrow(() -> new ResourceNotFoundException("La configuración de facturación no está disponible para la companyDto"));

        Billing billing = repository.findFirstByOrderByIdDesc();
        String prefixBilling = billingConfigDto.getPrefixBill();

        if (billing == null) {
            return prefixBilling.concat("-1");
        }

        Long consecutiveBilling = getBillingConsecutive(billing, prefixBilling);

        if (consecutiveBilling.intValue() >= billingConfigDto.getBillFrom() &&
                consecutiveBilling.intValue() <= billingConfigDto.getBillUntil()) {
            return prefixBilling.concat("-").concat(consecutiveBilling.toString());
        } else {
            throw new IllegalStateException("El número de factura esta por fuera del rango de facturación establecido.");
        }

    }

    private static Long getBillingConsecutive(Billing billing, String prefixBilling) {
        String billingNumber = billing.getBillNumber();
        if (billingNumber == null || !billingNumber.startsWith(prefixBilling)) {
            throw new IllegalStateException("El número de factura es inválido o no tiene el prefijo esperado");
        }

        String consecutiveNumber = billingNumber.replace(prefixBilling + "-", "");
        long consecutiveBilling;
        try {
            consecutiveBilling = Long.parseLong(consecutiveNumber) + 1L;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("El número de factura no es un número válido", e);
        }
        return consecutiveBilling;
    }

    private BigDecimal ivaTotalBill(List<SaleDetailDto> saleDetailDtos) {
        AtomicReference<BigDecimal> totalIvaBill = new AtomicReference<>(BigDecimal.ZERO);
        saleDetailDtos.forEach(sale -> totalIvaBill.set(totalIvaBill.get().add(sale.getTotalVat())));
        return totalIvaBill.get();
    }

    private BigDecimal subTotalBill(List<SaleDetailDto> saleDetailDtos) {
        AtomicReference<BigDecimal> subTotalBill = new AtomicReference<>(BigDecimal.ZERO);
        saleDetailDtos.forEach(sale -> subTotalBill.set(subTotalBill.get().add(sale.getSubTotal())));
        return subTotalBill.get();
    }

    private List<SaleDetailDto> getSaleDetail(OrderDto order) {
        List<SaleDetailDto> lstSaleDetailDto = new ArrayList<>();
        List<ProductDto> lstProducts = order.getProducts();
        if (!lstProducts.isEmpty()) {
            lstProducts.forEach(product -> {
                SaleDetailDto saleDetailDto = SaleDetailDto.builder()
                        .product(ProductApi.builder()
                                .id(product.getId())
                                .description(product.getDescription())
                                .vatType(product.getVatType())
                                .build())
                        .totalVat(getTotalIvaSale(product))
                        .build();
                lstSaleDetailDto.add(saleDetailDto);
            });
        }
        return lstSaleDetailDto;
    }

    private BigDecimal getTotalIvaSale(ProductDto product) {
        if (product.getVatType().name().equalsIgnoreCase(EVat.TARIFA_REDUCIDA.name())) {
            return ivaSale(product, EVat.TARIFA_REDUCIDA);
        }

        if (product.getVatType().name().equalsIgnoreCase(EVat.TARIFA_GENERAL.name())) {
            return ivaSale(product, EVat.TARIFA_GENERAL);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal ivaSale(ProductDto product, EVat vatValue) {
        if (product.getVatType().name().equalsIgnoreCase(vatValue.name())) {
            ProductVatTypeDto productIva = productVatTypeService.findByTipoIva(vatValue);
            BigDecimal iva = productIva.getPercentage().divide(new BigDecimal("100"));

            BigDecimal valorTotal = BigDecimal.ONE;
            return valorTotal.multiply(iva);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Valida los lotes antes de guardar la factura para evitar guardar la factura
     * si un lote no está disponible o no tiene stock suficiente.
     */
    private void validateBatchesForSale(List<SaleDetailDto> saleDetails) {
        if (saleDetails == null || saleDetails.isEmpty()) return;

        for (SaleDetailDto detail : saleDetails) {
            if (detail.getBatchId() == null || detail.getBatchId().isBlank()) continue;
            if (detail.getAmount() == null || detail.getAmount().compareTo(BigDecimal.ZERO) <= 0) continue;

            com.co.jarvis.entity.Batch batch = batchService.getBatchById(detail.getBatchId());

            if (batch.getStatus() == com.co.jarvis.enums.BatchStatus.DEPLETED
                    || batch.getStatus() == com.co.jarvis.enums.BatchStatus.CLOSED) {
                throw new SaveRecordException(
                        "El lote #" + batch.getBatchNumber() + " no está disponible para ventas. Estado: " + batch.getStatus());
            }

            if (batch.getStatus() == com.co.jarvis.enums.BatchStatus.EXPIRED
                    || (batch.getExpirationDate() != null
                            && batch.getExpirationDate().isBefore(java.time.LocalDate.now(com.co.jarvis.util.DateTimeUtil.getBogotaZone())))) {
                throw new SaveRecordException(
                        "El precio del lote #" + batch.getBatchNumber() + " está expirado. Actualice el precio antes de vender.");
            }

            int requested = detail.getAmount().intValue();
            int available = batch.getCurrentStock() != null ? batch.getCurrentStock() : 0;
            if (requested > available) {
                throw new SaveRecordException(String.format(
                        "Stock insuficiente en el lote #%d. Disponible: %d, solicitado: %d",
                        batch.getBatchNumber(), available, requested));
            }
        }
    }

    /**
     * Descuenta el stock de los productos vendidos.
     * Considera el fixedAmount de la presentación para productos a granel.
     */
    private void updateStockForSale(List<SaleDetailDto> saleDetails, String billingId, String userId) {
        logger.info("SaleServiceImpl -> updateStockForSale");
        
        if (saleDetails == null || saleDetails.isEmpty()) {
            logger.warn("No se proporcionaron detalles de venta para actualizar el stock");
            return;
        }

        for (SaleDetailDto detail : saleDetails) {
            if (detail.getProduct() == null || detail.getProduct().getId() == null) {
                logger.warn("Detalle de venta sin producto válido, omitiendo");
                continue;
            }
            
            if (detail.getAmount() == null || detail.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Detalle de venta sin cantidad válida para producto: {}", 
                    detail.getProduct().getDescription());
                continue;
            }

            String productId = detail.getProduct().getId();
            String barcode = detail.getProduct().getBarcode();
            BigDecimal amount = detail.getAmount();

            // Obtener el producto para calcular la cantidad real de stock a descontar
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                logger.warn("Producto no encontrado con ID: {}", productId);
                continue;
            }

            // Calcular cantidad real de stock a descontar
            Double stockQuantity = calculateStockQuantityForSale(product, barcode, amount);

            logger.info("Descontando stock para producto {}: cantidad vendida={}, stock a descontar={}",
                product.getProductCode(), amount, stockQuantity);

            // Registrar movimiento de venta y descontar stock
            inventoryService.registerSaleMovement(billingId, productId, stockQuantity, barcode, userId);

            // Si el producto es de categoría ANIMALES VIVOS y tiene batchId, descontar stock del lote
            if (detail.getBatchId() != null && !detail.getBatchId().isBlank()
                    && BatchConstants.BATCH_REQUIRED_CATEGORY.equalsIgnoreCase(product.getCategory())) {
                int batchQuantity = amount.intValue();
                logger.info("Descontando lote {} para producto ANIMALES VIVOS: cantidad={}",
                        detail.getBatchId(), batchQuantity);
                BatchSaleRequest batchSaleRequest = BatchSaleRequest.builder()
                        .batchId(detail.getBatchId())
                        .quantity(batchQuantity)
                        .billingId(billingId)
                        .build();
                batchService.registerSale(batchSaleRequest);
            }
        }
    }

    /**
     * Calcula la cantidad real de stock a descontar basada en el tipo de venta del producto.
     * Si el tipo de venta es diferente a UNIT, multiplica la cantidad por el fixedAmount de la presentación.
     * Ejemplo: Si se venden 5 kg de un producto que viene en bultos de 40 kg, se descuentan 5 kg del stock.
     * Pero si se vende 1 bulto, se descuentan 40 kg (1 * fixedAmount).
     */
    private Double calculateStockQuantityForSale(Product product, String barcode, BigDecimal amount) {
        logger.info("calculateStockQuantityForSale -> Producto: {}, SaleType: {}, Cantidad vendida: {}, Barcode: {}",
            product.getProductCode(), product.getSaleType(), amount, barcode);
        
        // Si el tipo de venta es UNIT o null, retornar la cantidad directamente
        if (product.getSaleType() == null || product.getSaleType() == ESale.UNIT) {
            logger.info("Producto {} con tipo de venta UNIT o null, usando cantidad directa: {}",
                product.getProductCode(), amount);
            return amount.doubleValue();
        }
        
        // Para productos a granel (WEIGHT, VOLUME, etc.), verificar si la presentación tiene fixedAmount
        // y si la venta fue por unidad de presentación (bulto) o por peso/volumen directo
        if (barcode != null && product.getPresentations() != null) {
            for (Presentation presentation : product.getPresentations()) {
                if (barcode.equals(presentation.getBarcode())) {
                    // Si la presentación tiene isFixedAmount=true y fixedAmount definido,
                    // significa que se vendió por unidad de presentación (ej: 1 bulto)
                    // y debemos multiplicar por el fixedAmount
                    if (Boolean.TRUE.equals(presentation.getIsFixedAmount()) && 
                        presentation.getFixedAmount() != null && 
                        presentation.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                        
                        BigDecimal realQuantity = amount.multiply(presentation.getFixedAmount());
                        logger.info("Producto {} vendido por presentación fija: {} unidades x {} (fixedAmount) = {} stock real",
                            product.getProductCode(), amount, presentation.getFixedAmount(), realQuantity);
                        return realQuantity.doubleValue();
                    }
                    break;
                }
            }
        }
        
        // Si no hay fixedAmount o la venta fue directa por peso/volumen, usar cantidad directa
        logger.info("Producto {} sin fixedAmount aplicable, usando cantidad directa: {}", 
            product.getProductCode(), amount);
        return amount.doubleValue();
    }

}
