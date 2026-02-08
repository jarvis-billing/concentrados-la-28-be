package com.co.jarvis.service.impl;

import com.co.jarvis.dto.PurchaseFilterDto;
import com.co.jarvis.dto.PurchaseInvoiceDto;
import com.co.jarvis.dto.PurchaseInvoiceItemDto;
import com.co.jarvis.entity.Presentation;
import com.co.jarvis.entity.Product;
import com.co.jarvis.entity.PurchaseInvoice;
import com.co.jarvis.entity.PurchaseInvoiceItem;
import com.co.jarvis.enums.ESale;
import com.co.jarvis.enums.EPurchaseInvoiceStatus;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.repository.PurchaseInvoiceRepository;
import com.co.jarvis.service.PurchaseInvoiceService;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import com.co.jarvis.util.exception.DeleteRecordException;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PurchaseInvoiceServiceImpl implements PurchaseInvoiceService {

    @Autowired
    private PurchaseInvoiceRepository purchaseInvoiceRepository;

    @Autowired
    private ProductRepository productRepository;

    GenericMapper<PurchaseInvoice, PurchaseInvoiceDto> mapper = 
        new GenericMapper<>(PurchaseInvoice.class, PurchaseInvoiceDto.class);

    GenericMapper<PurchaseInvoiceItem, PurchaseInvoiceItemDto> itemMapper = 
        new GenericMapper<>(PurchaseInvoiceItem.class, PurchaseInvoiceItemDto.class);

    @Override
    public List<PurchaseInvoiceDto> list(PurchaseFilterDto filter) {
        log.info("PurchaseInvoiceServiceImpl -> list");
        try {
            List<PurchaseInvoice> invoices;

            if (filter == null) {
                invoices = purchaseInvoiceRepository.findAll();
            } else if (filter.getDateFrom() != null && filter.getDateTo() != null) {
                invoices = purchaseInvoiceRepository.findByDateBetween(
                    filter.getDateFrom(), 
                    filter.getDateTo()
                );
            } else if (filter.getSupplier() != null && filter.getSupplier().getId() != null) {
                invoices = purchaseInvoiceRepository.findBySupplierId(filter.getSupplier().getId());
            } else if (filter.getInvoiceNumber() != null) {
                PurchaseInvoice invoice = purchaseInvoiceRepository.findByInvoiceNumber(
                    filter.getInvoiceNumber()
                );
                invoices = invoice != null ? List.of(invoice) : List.of();
            } else {
                invoices = purchaseInvoiceRepository.findAll();
            }

            return invoices.stream()
                .map(mapper::mapToDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("PurchaseInvoiceServiceImpl -> list -> ERROR: {}", e.getMessage());
            throw new SaveRecordException("Error al listar facturas de compra");
        }
    }

    @Override
    public PurchaseInvoiceDto findById(String id) {
        log.info("PurchaseInvoiceServiceImpl -> findById: {}", id);
        Optional<PurchaseInvoice> invoice = purchaseInvoiceRepository.findById(id);
        return invoice.map(mapper::mapToDto).orElse(null);
    }

    @Override
    @Transactional
    public PurchaseInvoiceDto create(PurchaseInvoiceDto dto) {
        log.info("PurchaseInvoiceServiceImpl -> create");
        try {
            // Validaciones básicas
            validatePurchaseInvoice(dto);

            // Mapear DTO a entidad
            PurchaseInvoice invoice = mapper.mapToEntity(dto);
            
            // Configurar valores por defecto
            invoice.setStatus(EPurchaseInvoiceStatus.CREATED);
            invoice.setCreatedAt(OffsetDateTime.now());
            invoice.setUpdatedAt(OffsetDateTime.now());

            // Calcular total si no viene
            if (invoice.getTotalAmount() == null) {
                invoice.setTotalAmount(calculateTotal(invoice.getItems()));
            }

            // Guardar factura de compra
            invoice = purchaseInvoiceRepository.save(invoice);

            // Actualizar stock de productos
            updateStockForCreation(invoice.getItems());

            log.info("PurchaseInvoiceServiceImpl -> create -> Factura creada con ID: {}", invoice.getId());
            return mapper.mapToDto(invoice);

        } catch (ResourceNotFoundException e) {
            log.error("PurchaseInvoiceServiceImpl -> create -> ERROR: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PurchaseInvoiceServiceImpl -> create -> ERROR: {}", e.getMessage());
            throw new SaveRecordException("Error al crear factura de compra: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PurchaseInvoiceDto update(String id, PurchaseInvoiceDto dto) {
        log.info("PurchaseInvoiceServiceImpl -> update: {}", id);
        try {
            // Obtener factura original
            PurchaseInvoice originalInvoice = purchaseInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));

            // Validaciones básicas
            validatePurchaseInvoice(dto);

            // Ajustar stock según diferencias entre original y nueva versión
            adjustStockForUpdate(originalInvoice.getItems(), dto.getItems());

            // Mapear DTO a entidad
            PurchaseInvoice updatedInvoice = mapper.mapToEntity(dto);
            updatedInvoice.setId(id);
            updatedInvoice.setCreatedAt(originalInvoice.getCreatedAt());
            updatedInvoice.setUpdatedAt(OffsetDateTime.now());

            // Calcular total si no viene
            if (updatedInvoice.getTotalAmount() == null) {
                updatedInvoice.setTotalAmount(calculateTotal(updatedInvoice.getItems()));
            }

            // Guardar factura actualizada
            updatedInvoice = purchaseInvoiceRepository.save(updatedInvoice);

            log.info("PurchaseInvoiceServiceImpl -> update -> Factura actualizada con ID: {}", id);
            return mapper.mapToDto(updatedInvoice);

        } catch (ResourceNotFoundException e) {
            log.error("PurchaseInvoiceServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PurchaseInvoiceServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException("Error al actualizar factura de compra: " + e.getMessage());
        }
    }

    @Override
    public void deleteById(String id) {
        log.info("PurchaseInvoiceServiceImpl -> deleteById: {}", id);
        try {
            purchaseInvoiceRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            log.error("PurchaseInvoiceServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("PurchaseInvoiceServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new DeleteRecordException(MessageConstants.DELETE_RECORD_ERROR);
        }
    }

    @Override
    @Transactional
    public PurchaseInvoiceDto addItems(String id, List<PurchaseInvoiceItemDto> newItems) {
        log.info("PurchaseInvoiceServiceImpl -> addItems: {}", id);
        try {
            // Buscar la factura por ID
            PurchaseInvoice invoice = purchaseInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

            // Validar que se proporcionaron items
            if (newItems == null || newItems.isEmpty()) {
                throw new SaveRecordException("Debe proporcionar al menos un item");
            }

            // Validar cada item nuevo
            for (PurchaseInvoiceItemDto item : newItems) {
                if (item.getProductId() == null && item.getProductCode() == null) {
                    throw new SaveRecordException("Cada item debe tener productId o productCode");
                }
                if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new SaveRecordException("La cantidad debe ser mayor a cero");
                }
                if (item.getUnitCost() == null || item.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                    throw new SaveRecordException("El costo unitario no puede ser negativo");
                }
            }

            // Convertir DTOs a entidades y agregar a la factura
            List<PurchaseInvoiceItem> newItemEntities = newItems.stream()
                .map(itemMapper::mapToEntity)
                .collect(Collectors.toList());

            // Agregar nuevos items al array de items existente
            invoice.getItems().addAll(newItemEntities);

            // Recalcular el total de la factura
            invoice.setTotalAmount(calculateTotal(invoice.getItems()));
            invoice.setUpdatedAt(OffsetDateTime.now());

            // Guardar la factura actualizada
            invoice = purchaseInvoiceRepository.save(invoice);

            // Actualizar stock de productos para los nuevos items
            updateStockForCreation(newItemEntities);

            log.info("PurchaseInvoiceServiceImpl -> addItems -> {} items agregados a factura {}", 
                newItems.size(), id);
            return mapper.mapToDto(invoice);

        } catch (ResourceNotFoundException e) {
            log.error("PurchaseInvoiceServiceImpl -> addItems -> ERROR: {}", e.getMessage());
            throw e;
        } catch (SaveRecordException e) {
            log.error("PurchaseInvoiceServiceImpl -> addItems -> ERROR: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PurchaseInvoiceServiceImpl -> addItems -> ERROR: {}", e.getMessage());
            throw new SaveRecordException("Error al agregar items a la factura: " + e.getMessage());
        }
    }

    /**
     * Valida los datos básicos de una factura de compra
     */
    private void validatePurchaseInvoice(PurchaseInvoiceDto dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new SaveRecordException("La factura de compra debe tener al menos un item");
        }

        for (PurchaseInvoiceItemDto item : dto.getItems()) {
            if (item.getProductId() == null && item.getProductCode() == null) {
                throw new SaveRecordException("Cada item debe tener productId o productCode");
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new SaveRecordException("La cantidad debe ser mayor a cero");
            }
            if (item.getUnitCost() == null || item.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new SaveRecordException("El costo unitario no puede ser negativo");
            }
        }
    }

    /**
     * Calcula el total de la factura sumando los totalCost de los items
     */
    private BigDecimal calculateTotal(List<PurchaseInvoiceItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
            .map(item -> {
                BigDecimal total = item.getTotalCost();
                if (total == null) {
                    total = item.getQuantity().multiply(item.getUnitCost());
                }
                return total;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Actualiza el stock de productos al crear una factura de compra.
     * Incrementa el stock sumando la cantidad comprada de cada item.
     * Si el producto tiene tipo de venta diferente a UNIT, multiplica la cantidad por el fixedAmount de la presentación.
     */
    private void updateStockForCreation(List<PurchaseInvoiceItem> items) {
        log.info("PurchaseInvoiceServiceImpl -> updateStockForCreation");
        
        if (items == null || items.isEmpty()) {
            log.warn("No se proporcionaron items para actualizar el stock");
            throw new SaveRecordException("No se proporcionaron items para actualizar el stock");
        }

        for (PurchaseInvoiceItem item : items) {
            Product product = findProductByItem(item);
            
            if (product == null) {
                log.warn("Producto no encontrado para item: {}", item.getProductId());
                throw new ResourceNotFoundException(
                    "Producto no encontrado: " + 
                    (item.getProductCode() != null ? item.getProductCode() : item.getProductId())
                );
            }

            // Obtener stock actual antes de incrementar
            BigDecimal stockAntes = product.getStock() != null ? product.getStock().getQuantity() : BigDecimal.ZERO;
            log.info("Stock ANTES para producto {}: {}", product.getProductCode(), stockAntes);

            // Calcular cantidad real de stock a incrementar
            BigDecimal stockQuantity = calculateStockQuantity(product, item);

            // Incrementar stock
            product.increaseStock(stockQuantity);
            product.updatePresentationCost(item.getPresentationBarcode(), item.getUnitCost());
            
            // Obtener stock después de incrementar
            BigDecimal stockDespues = product.getStock().getQuantity();
            log.info("Stock DESPUÉS para producto {}: {} (incrementó: {})", 
                product.getProductCode(), stockDespues, stockQuantity);
            
            productRepository.save(product);
            
            log.info("Stock incrementado para producto {}: +{} (cantidad ingresada: {}, stock final: {})", 
                product.getProductCode(), stockQuantity, item.getQuantity(), stockDespues);
        }
    }

    /**
     * Calcula la cantidad real de stock basada en el tipo de venta del producto.
     * Si el tipo de venta es diferente a UNIT, multiplica la cantidad por el fixedAmount de la presentación.
     * Ejemplo: 20 bultos x 40 kg (fixedAmount) = 800 kg de stock.
     */
    private BigDecimal calculateStockQuantity(Product product, PurchaseInvoiceItem item) {
        BigDecimal quantity = item.getQuantity();
        
        log.info("calculateStockQuantity -> Producto: {}, SaleType: {}, Cantidad ingresada: {}",
            product.getProductCode(), product.getSaleType(), quantity);
        
        // Si el tipo de venta es UNIT o null, retornar la cantidad directamente
        if (product.getSaleType() == null || product.getSaleType() == ESale.UNIT) {
            log.info("Producto {} con tipo de venta UNIT o null, usando cantidad directa: {}",
                product.getProductCode(), quantity);
            return quantity;
        }
        
        // Para otros tipos de venta (WEIGHT, VOLUME, etc.), buscar el fixedAmount de la presentación
        BigDecimal fixedAmount = findFixedAmountForItem(product, item);
        
        log.info("Producto {} -> fixedAmount encontrado: {}", product.getProductCode(), fixedAmount);
        
        if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal realQuantity = quantity.multiply(fixedAmount);
            log.info("Producto {} con tipo de venta {}: {} unidades x {} (fixedAmount) = {} stock real",
                product.getProductCode(), product.getSaleType(), quantity, fixedAmount, realQuantity);
            return realQuantity;
        }
        
        // Si no hay fixedAmount, retornar la cantidad original
        log.warn("Producto {} sin fixedAmount definido, usando cantidad directa: {}", 
            product.getProductCode(), quantity);
        return quantity;
    }

    /**
     * Busca el fixedAmount de la presentación correspondiente al item.
     * Primero busca por presentationBarcode, luego por la primera presentación con fixedAmount definido.
     */
    private BigDecimal findFixedAmountForItem(Product product, PurchaseInvoiceItem item) {
        if (product.getPresentations() == null || product.getPresentations().isEmpty()) {
            log.info("findFixedAmountForItem -> Producto {} sin presentaciones", product.getProductCode());
            return null;
        }
        
        log.info("findFixedAmountForItem -> Producto {} tiene {} presentaciones, presentationBarcode del item: {}",
            product.getProductCode(), product.getPresentations().size(), item.getPresentationBarcode());
        
        // Si se especificó un barcode de presentación, buscar esa presentación específica
        if (item.getPresentationBarcode() != null) {
            for (Presentation presentation : product.getPresentations()) {
                log.info("findFixedAmountForItem -> Comparando barcode: {} con {}, fixedAmount: {}",
                    item.getPresentationBarcode(), presentation.getBarcode(), presentation.getFixedAmount());
                if (item.getPresentationBarcode().equals(presentation.getBarcode())) {
                    // Usar fixedAmount si existe, independientemente de isFixedAmount
                    if (presentation.getFixedAmount() != null && presentation.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                        log.info("findFixedAmountForItem -> Encontrado fixedAmount por barcode: {}", presentation.getFixedAmount());
                        return presentation.getFixedAmount();
                    }
                }
            }
        }
        
        // Si no se encontró por barcode, buscar la primera presentación con fixedAmount definido
        for (Presentation presentation : product.getPresentations()) {
            log.info("findFixedAmountForItem -> Revisando presentación: barcode={}, fixedAmount={}",
                presentation.getBarcode(), presentation.getFixedAmount());
            if (presentation.getFixedAmount() != null && presentation.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                log.info("findFixedAmountForItem -> Usando primera presentación con fixedAmount: {}", presentation.getFixedAmount());
                return presentation.getFixedAmount();
            }
        }
        
        log.warn("findFixedAmountForItem -> No se encontró fixedAmount para producto {}", product.getProductCode());
        return null;
    }

    /**
     * Ajusta el stock de productos al actualizar una factura de compra.
     * Calcula las diferencias entre la versión original y la nueva versión,
     * y ajusta el stock en consecuencia.
     * Considera el fixedAmount para productos con tipo de venta diferente a UNIT.
     */
    private void adjustStockForUpdate(
        List<PurchaseInvoiceItem> originalItems, 
        List<PurchaseInvoiceItemDto> newItemsDto
    ) {
        log.info("PurchaseInvoiceServiceImpl -> adjustStockForUpdate");

        // Convertir DTOs a entidades para facilitar el procesamiento
        List<PurchaseInvoiceItem> newItems = newItemsDto.stream()
            .map(itemMapper::mapToEntity)
            .collect(Collectors.toList());

        // Crear mapas para facilitar comparaciones: clave = productId o productCode
        // Los mapas ahora contienen las cantidades reales de stock (considerando fixedAmount)
        Map<String, BigDecimal> originalQuantities = buildStockQuantityMap(originalItems);
        Map<String, BigDecimal> newQuantities = buildStockQuantityMap(newItems);

        // Obtener todas las claves únicas (productos afectados)
        Map<String, BigDecimal> allKeys = new HashMap<>();
        allKeys.putAll(originalQuantities);
        allKeys.putAll(newQuantities);

        // Procesar cada producto afectado
        for (String key : allKeys.keySet()) {
            BigDecimal originalQty = originalQuantities.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal newQty = newQuantities.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal difference = newQty.subtract(originalQty);

            // Si no hay cambio, continuar
            if (difference.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Buscar el producto
            Product product = findProductByKey(key);
            if (product == null) {
                log.warn("Producto no encontrado para clave: {}", key);
                throw new ResourceNotFoundException("Producto no encontrado: " + key);
            }

            // Ajustar stock según la diferencia (ya está calculada con fixedAmount)
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                // Incremento: se agregó más cantidad o es un producto nuevo
                product.increaseStock(difference);
                log.info("Stock incrementado para producto {}: +{}", 
                    product.getProductCode(), difference);
            } else {
                // Decremento: se redujo la cantidad o se eliminó el producto
                product.reduceStock(difference.abs());
                log.info("Stock reducido para producto {}: -{}", 
                    product.getProductCode(), difference.abs());
            }

            productRepository.save(product);
        }
    }

    /**
     * Construye un mapa de cantidades de stock reales por producto (key = productId o productCode).
     * Considera el fixedAmount para productos con tipo de venta diferente a UNIT.
     */
    private Map<String, BigDecimal> buildStockQuantityMap(List<PurchaseInvoiceItem> items) {
        Map<String, BigDecimal> map = new HashMap<>();
        
        if (items == null) {
            return map;
        }

        for (PurchaseInvoiceItem item : items) {
            String key = item.getProductId() != null ? 
                item.getProductId() : item.getProductCode();
            
            // Buscar el producto para calcular la cantidad real de stock
            Product product = findProductByKey(key);
            BigDecimal stockQuantity;
            
            if (product != null) {
                stockQuantity = calculateStockQuantity(product, item);
            } else {
                // Si no se encuentra el producto, usar la cantidad directa
                stockQuantity = item.getQuantity();
            }
            
            BigDecimal currentQty = map.getOrDefault(key, BigDecimal.ZERO);
            map.put(key, currentQty.add(stockQuantity));
        }
        
        return map;
    }

    /**
     * Busca un producto por item (primero por productId, luego por productCode)
     */
    private Product findProductByItem(PurchaseInvoiceItem item) {
        if (item.getProductId() != null) {
            return productRepository.findById(Objects.requireNonNull(item.getProductId())).orElse(null);
        } else if (item.getProductCode() != null) {
            // Buscar por productCode usando query
            List<Product> products = productRepository.findAll();
            return products.stream()
                .filter(p -> item.getProductCode().equals(p.getProductCode()))
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    /**
     * Busca un producto por clave (puede ser productId o productCode)
     */
    private Product findProductByKey(String key) {
        // Intentar buscar por ID primero
        Optional<Product> productById = productRepository.findById(key);
        if (productById.isPresent()) {
            return productById.get();
        }
        
        // Si no se encuentra por ID, buscar por productCode
        List<Product> products = productRepository.findAll();
        return products.stream()
            .filter(p -> key.equals(p.getProductCode()))
            .findFirst()
            .orElse(null);
    }
}
