package com.co.jarvis.service.impl;

import com.co.jarvis.dto.PurchaseFilterDto;
import com.co.jarvis.dto.PurchaseInvoiceDto;
import com.co.jarvis.dto.PurchaseInvoiceItemDto;
import com.co.jarvis.entity.Product;
import com.co.jarvis.entity.PurchaseInvoice;
import com.co.jarvis.entity.PurchaseInvoiceItem;
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
            } else if (filter.getSupplierId() != null) {
                invoices = purchaseInvoiceRepository.findBySupplierId(filter.getSupplierId());
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
     */
    private void updateStockForCreation(List<PurchaseInvoiceItem> items) {
        log.info("PurchaseInvoiceServiceImpl -> updateStockForCreation");
        
        if (items == null || items.isEmpty()) {
            return;
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

            // Incrementar stock
            product.increaseStock(item.getQuantity());
            productRepository.save(product);
            
            log.info("Stock incrementado para producto {}: +{}", 
                product.getProductCode(), item.getQuantity());
        }
    }

    /**
     * Ajusta el stock de productos al actualizar una factura de compra.
     * Calcula las diferencias entre la versión original y la nueva versión,
     * y ajusta el stock en consecuencia.
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
        Map<String, BigDecimal> originalQuantities = buildQuantityMap(originalItems);
        Map<String, BigDecimal> newQuantities = buildQuantityMap(newItems);

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

            // Ajustar stock según la diferencia
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
     * Construye un mapa de cantidades por producto (key = productId o productCode)
     */
    private Map<String, BigDecimal> buildQuantityMap(List<PurchaseInvoiceItem> items) {
        Map<String, BigDecimal> map = new HashMap<>();
        
        if (items == null) {
            return map;
        }

        for (PurchaseInvoiceItem item : items) {
            String key = item.getProductId() != null ? 
                item.getProductId() : item.getProductCode();
            
            BigDecimal currentQty = map.getOrDefault(key, BigDecimal.ZERO);
            map.put(key, currentQty.add(item.getQuantity()));
        }
        
        return map;
    }

    /**
     * Busca un producto por item (primero por productId, luego por productCode)
     */
    private Product findProductByItem(PurchaseInvoiceItem item) {
        if (item.getProductId() != null) {
            return productRepository.findById(item.getProductId()).orElse(null);
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
