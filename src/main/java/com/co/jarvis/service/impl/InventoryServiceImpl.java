package com.co.jarvis.service.impl;

import com.co.jarvis.dto.InventoryDashboardDto;
import com.co.jarvis.dto.StockAlertDto;
import com.co.jarvis.entity.*;
import com.co.jarvis.enums.*;
import com.co.jarvis.repository.*;
import com.co.jarvis.service.InventoryService;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryMovementRepository movementRepository;

    @Autowired
    private PhysicalInventoryRepository physicalInventoryRepository;

    @Autowired
    private InventoryAdjustmentRepository adjustmentRepository;

    @Autowired
    private ProductRepository productRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ========== MOVIMIENTOS ==========

    @Override
    public List<InventoryMovement> getMovements(String startDate, String endDate, String productId,
                                                 EMovementType movementType, String userId) {
        log.info("InventoryService -> getMovements");
        
        if (productId != null && movementType != null && startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
            return movementRepository.findByFilters(productId, movementType, start, end);
        }
        
        if (productId != null && startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
            return movementRepository.findByProductIdAndDateBetween(productId, start, end);
        }
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
            return movementRepository.findByDateBetween(start, end);
        }
        
        if (productId != null) {
            return movementRepository.findByProductId(productId);
        }
        
        if (movementType != null) {
            return movementRepository.findByMovementType(movementType);
        }
        
        if (userId != null) {
            return movementRepository.findByUserId(userId);
        }
        
        return movementRepository.findAllByOrderByDateDesc();
    }

    @Override
    public List<InventoryMovement> getMovementsByProduct(String productId) {
        log.info("InventoryService -> getMovementsByProduct: {}", productId);
        return movementRepository.findByProductId(productId);
    }

    @Override
    public InventoryMovement getMovementById(String id) {
        log.info("InventoryService -> getMovementById: {}", id);
        return movementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public InventoryMovement createMovement(InventoryMovement movement) {
        log.info("InventoryService -> createMovement");
        
        Product product = productRepository.findById(Objects.requireNonNull(movement.getProductId()))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        // Establecer referencia del producto
        movement.setProduct(buildProductReference(product));
        
        // Establecer stock anterior
        movement.setPreviousStock(product.getStock().getQuantity().doubleValue());
        
        // Calcular nuevo stock según tipo de movimiento
        Double newStock = calculateNewStock(movement.getPreviousStock(), movement.getQuantity(), movement.getMovementType());
        movement.setNewStock(newStock);
        
        // Actualizar stock del producto
        product.getStock().setQuantity(BigDecimal.valueOf(newStock));
        productRepository.save(product);
        
        // Establecer unidad de medida
        if (movement.getUnitMeasure() == null && product.getStock().getUnitMeasure() != null) {
            movement.setUnitMeasure(product.getStock().getUnitMeasure().name());
        }
        
        movement.setCreatedAt(LocalDateTime.now());
        if (movement.getDate() == null) {
            movement.setDate(LocalDateTime.now());
        }
        
        return movementRepository.save(movement);
    }

    // ========== INVENTARIO FÍSICO ==========

    @Override
    public List<PhysicalInventory> getPhysicalInventories(String startDate, String endDate,
                                                           String productId, EAdjustmentReason adjustmentReason) {
        log.info("InventoryService -> getPhysicalInventories");
        
        if (productId != null && startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
            return physicalInventoryRepository.findByProductIdAndDateBetween(productId, start, end);
        }
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
            return physicalInventoryRepository.findByDateBetween(start, end);
        }
        
        if (productId != null) {
            return physicalInventoryRepository.findByProductId(productId);
        }
        
        if (adjustmentReason != null) {
            return physicalInventoryRepository.findByAdjustmentReason(adjustmentReason);
        }
        
        return physicalInventoryRepository.findAllByOrderByDateDesc();
    }

    @Override
    public PhysicalInventory getPhysicalInventoryById(String id) {
        log.info("InventoryService -> getPhysicalInventoryById: {}", id);
        return physicalInventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario físico no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public PhysicalInventory createPhysicalInventory(PhysicalInventory inventory) {
        log.info("InventoryService -> createPhysicalInventory");
        
        // 1. Obtener producto
        Product product = productRepository.findById(Objects.requireNonNull(inventory.getProductId()))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        // 2. Establecer referencia del producto
        inventory.setProduct(buildProductReference(product));
        
        // 3. Obtener stock actual del sistema
        inventory.setSystemStock(product.getStock().getQuantity().doubleValue());
        
        // 4. Calcular diferencia: physicalStock - systemStock
        Double difference = inventory.getPhysicalStock() - inventory.getSystemStock();
        inventory.setDifference(difference);
        
        // 5. Actualizar stock del producto con el valor físico
        product.getStock().setQuantity(BigDecimal.valueOf(inventory.getPhysicalStock()));
        productRepository.save(product);
        
        // 6. Guardar inventario físico
        inventory.setCreatedAt(LocalDateTime.now());
        if (inventory.getDate() == null) {
            inventory.setDate(LocalDateTime.now());
        }
        PhysicalInventory saved = physicalInventoryRepository.save(inventory);
        
        // 7. Crear movimiento de inventario tipo AJUSTE_FISICO
        InventoryMovement movement = InventoryMovement.builder()
                .date(inventory.getDate())
                .productId(inventory.getProductId())
                .product(inventory.getProduct())
                .presentationBarcode(inventory.getPresentationBarcode())
                .movementType(EMovementType.AJUSTE_FISICO)
                .quantity(difference)
                .previousStock(inventory.getSystemStock())
                .newStock(inventory.getPhysicalStock())
                .unitMeasure(product.getStock().getUnitMeasure() != null ? 
                        product.getStock().getUnitMeasure().name() : null)
                .reference("FISICO-" + saved.getId())
                .userId(inventory.getUserId())
                .user(inventory.getUser())
                .notes(inventory.getNotes())
                .createdAt(LocalDateTime.now())
                .build();
        movementRepository.save(movement);
        
        return saved;
    }

    // ========== AJUSTES ==========

    @Override
    public List<InventoryAdjustment> getAdjustments(String startDate, String endDate, String productId,
                                                     EAdjustmentType adjustmentType, EAdjustmentReason reason) {
        log.info("InventoryService -> getAdjustments");
        
        if (productId != null && startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
            return adjustmentRepository.findByProductIdAndDateBetween(productId, start, end);
        }
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
            return adjustmentRepository.findByDateBetween(start, end);
        }
        
        if (productId != null) {
            return adjustmentRepository.findByProductId(productId);
        }
        
        if (adjustmentType != null) {
            return adjustmentRepository.findByAdjustmentType(adjustmentType);
        }
        
        if (reason != null) {
            return adjustmentRepository.findByReason(reason);
        }
        
        return adjustmentRepository.findAllByOrderByDateDesc();
    }

    @Override
    public InventoryAdjustment getAdjustmentById(String id) {
        log.info("InventoryService -> getAdjustmentById: {}", id);
        return adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ajuste no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public InventoryAdjustment createAdjustment(InventoryAdjustment adjustment) {
        log.info("InventoryService -> createAdjustment");
        
        // 1. Obtener producto
        Product product = productRepository.findById(Objects.requireNonNull(adjustment.getProductId()))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        // 2. Establecer referencia del producto
        adjustment.setProduct(buildProductReference(product));
        
        // 3. Validar que la cantidad sea positiva
        if (adjustment.getQuantity() == null || adjustment.getQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        
        // 4. Obtener stock anterior
        Double previousStock = product.getStock().getQuantity().doubleValue();
        adjustment.setPreviousStock(previousStock);
        
        // 5. Calcular nuevo stock según tipo
        Double newStock;
        if (adjustment.getAdjustmentType() == EAdjustmentType.INCREMENT) {
            newStock = previousStock + adjustment.getQuantity();
        } else {
            newStock = previousStock - adjustment.getQuantity();
        }
        
        // 6. Validar que el nuevo stock no sea negativo
        if (newStock < 0) {
            throw new IllegalArgumentException("El ajuste resultaría en stock negativo");
        }
        
        adjustment.setNewStock(newStock);
        
        // 7. Actualizar stock del producto
        product.getStock().setQuantity(BigDecimal.valueOf(newStock));
        productRepository.save(product);
        
        // 8. Guardar ajuste
        adjustment.setCreatedAt(LocalDateTime.now());
        if (adjustment.getDate() == null) {
            adjustment.setDate(LocalDateTime.now());
        }
        InventoryAdjustment saved = adjustmentRepository.save(adjustment);
        
        // 9. Crear movimiento de inventario tipo AJUSTE_MANUAL
        InventoryMovement movement = InventoryMovement.builder()
                .date(adjustment.getDate())
                .productId(adjustment.getProductId())
                .product(adjustment.getProduct())
                .presentationBarcode(adjustment.getPresentationBarcode())
                .movementType(EMovementType.AJUSTE_MANUAL)
                .quantity(adjustment.getAdjustmentType() == EAdjustmentType.INCREMENT ? 
                        adjustment.getQuantity() : -adjustment.getQuantity())
                .previousStock(previousStock)
                .newStock(newStock)
                .unitMeasure(product.getStock().getUnitMeasure() != null ? 
                        product.getStock().getUnitMeasure().name() : null)
                .reference("AJUSTE-" + saved.getId())
                .userId(adjustment.getUserId())
                .user(adjustment.getUser())
                .notes(adjustment.getNotes())
                .createdAt(LocalDateTime.now())
                .build();
        movementRepository.save(movement);
        
        return saved;
    }

    @Override
    @Transactional
    public InventoryAdjustment authorizeAdjustment(String id, String authorizedBy) {
        log.info("InventoryService -> authorizeAdjustment: {}", id);
        
        InventoryAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ajuste no encontrado con ID: " + id));
        
        adjustment.setAuthorizedBy(authorizedBy);
        adjustment.setAuthorizedAt(LocalDateTime.now());
        
        return adjustmentRepository.save(adjustment);
    }

    // ========== DASHBOARD ==========

    @Override
    public InventoryDashboardDto getDashboard() {
        log.info("InventoryService -> getDashboard");
        
        List<Product> allProducts = productRepository.findAll();
        
        // Total de productos
        int totalProducts = allProducts.size();
        
        // Calcular valor total del inventario
        BigDecimal totalValue = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getPresentations() != null && !p.getPresentations().isEmpty())
                .map(p -> {
                    BigDecimal qty = p.getStock().getQuantity() != null ? p.getStock().getQuantity() : BigDecimal.ZERO;
                    BigDecimal cost = p.getPresentations().get(0).getCostPrice() != null ? 
                            p.getPresentations().get(0).getCostPrice() : BigDecimal.ZERO;
                    return qty.multiply(cost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Contar productos por nivel de stock
        int outOfStock = 0;
        int criticalStock = 0;
        int lowStock = 0;
        
        for (Product product : allProducts) {
            if (product.getStock() != null && product.getStock().getQuantity() != null) {
                double qty = product.getStock().getQuantity().doubleValue();
                if (qty <= 0) {
                    outOfStock++;
                } else if (qty <= 5) {
                    criticalStock++;
                } else if (qty <= 10) {
                    lowStock++;
                }
            }
        }
        
        // Obtener alertas de stock
        List<StockAlertDto> alerts = getStockAlerts();
        
        return InventoryDashboardDto.builder()
                .totalProducts(totalProducts)
                .totalInventoryValue(totalValue)
                .outOfStockProducts(outOfStock)
                .criticalStockProducts(criticalStock)
                .lowStockProducts(lowStock)
                .stockAlerts(alerts)
                .topSellingProducts(new ArrayList<>())
                .lowRotationProducts(new ArrayList<>())
                .build();
    }

    @Override
    public List<StockAlertDto> getStockAlerts() {
        log.info("InventoryService -> getStockAlerts");
        
        List<Product> products = productRepository.findAll();
        List<StockAlertDto> alerts = new ArrayList<>();
        
        for (Product product : products) {
            if (product.getStock() == null || product.getStock().getQuantity() == null) {
                continue;
            }
            
            double currentStock = product.getStock().getQuantity().doubleValue();
            double minimumStock = 10.0; // Valor por defecto
            double criticalStock = 5.0; // Valor por defecto
            
            EAlertLevel alertLevel;
            if (currentStock <= 0) {
                alertLevel = EAlertLevel.OUT_OF_STOCK;
            } else if (currentStock <= criticalStock) {
                alertLevel = EAlertLevel.CRITICAL;
            } else if (currentStock <= minimumStock) {
                alertLevel = EAlertLevel.LOW;
            } else {
                continue; // No agregar alerta si el stock es normal
            }
            
            String barcode = "";
            if (product.getPresentations() != null && !product.getPresentations().isEmpty()) {
                barcode = product.getPresentations().get(0).getBarcode();
            }
            
            alerts.add(StockAlertDto.builder()
                    .productId(product.getId())
                    .productName(product.getDescription())
                    .barcode(barcode)
                    .currentStock(currentStock)
                    .minimumStock(minimumStock)
                    .criticalStock(criticalStock)
                    .unitMeasure(product.getStock().getUnitMeasure() != null ? 
                            product.getStock().getUnitMeasure().name() : null)
                    .alertLevel(alertLevel)
                    .build());
        }
        
        return alerts;
    }

    // ========== REPORTES ==========

    @Override
    public Map<String, Object> getRotationReport(String startDate, String endDate) {
        log.info("InventoryService -> getRotationReport");
        
        Map<String, Object> report = new HashMap<>();
        
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, DATE_FORMATTER) : 
                LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, DATE_FORMATTER) : 
                LocalDateTime.now();
        
        // Obtener movimientos de venta en el período
        List<InventoryMovement> salesMovements = movementRepository
                .findByMovementTypeAndDateBetween(EMovementType.VENTA, start, end);
        
        // Agrupar por producto y sumar cantidades
        Map<String, Double> salesByProduct = salesMovements.stream()
                .collect(Collectors.groupingBy(
                        InventoryMovement::getProductId,
                        Collectors.summingDouble(m -> Math.abs(m.getQuantity()))
                ));
        
        report.put("period", Map.of("start", start, "end", end));
        report.put("salesByProduct", salesByProduct);
        report.put("totalMovements", salesMovements.size());
        
        return report;
    }

    @Override
    public Map<String, Object> getValuationReport() {
        log.info("InventoryService -> getValuationReport");
        
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> productValuations = new ArrayList<>();
        
        List<Product> products = productRepository.findAll();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (Product product : products) {
            if (product.getStock() == null || product.getPresentations() == null || 
                    product.getPresentations().isEmpty()) {
                continue;
            }
            
            BigDecimal qty = product.getStock().getQuantity() != null ? 
                    product.getStock().getQuantity() : BigDecimal.ZERO;
            BigDecimal cost = product.getPresentations().get(0).getCostPrice() != null ? 
                    product.getPresentations().get(0).getCostPrice() : BigDecimal.ZERO;
            BigDecimal value = qty.multiply(cost);
            
            totalValue = totalValue.add(value);
            
            Map<String, Object> productVal = new HashMap<>();
            productVal.put("productId", product.getId());
            productVal.put("description", product.getDescription());
            productVal.put("quantity", qty);
            productVal.put("costPrice", cost);
            productVal.put("totalValue", value);
            productValuations.add(productVal);
        }
        
        report.put("products", productValuations);
        report.put("totalInventoryValue", totalValue);
        report.put("generatedAt", LocalDateTime.now());
        
        return report;
    }

    @Override
    public Map<String, Object> getABCReport() {
        log.info("InventoryService -> getABCReport");
        
        Map<String, Object> report = new HashMap<>();
        
        // Obtener movimientos de venta de los últimos 3 meses
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<InventoryMovement> salesMovements = movementRepository
                .findByMovementTypeAndDateBetween(EMovementType.VENTA, threeMonthsAgo, LocalDateTime.now());
        
        // Agrupar por producto y sumar valor de ventas
        Map<String, Double> salesValueByProduct = new HashMap<>();
        for (InventoryMovement movement : salesMovements) {
            String productId = movement.getProductId();
            Double quantity = Math.abs(movement.getQuantity());
            
            // Obtener precio del producto
            Product product = productId != null ? productRepository.findById(productId).orElse(null) : null;
            if (product != null && product.getPresentations() != null && !product.getPresentations().isEmpty()) {
                BigDecimal price = product.getPresentations().get(0).getSalePrice();
                if (price != null) {
                    double value = quantity * price.doubleValue();
                    salesValueByProduct.merge(productId, value, Double::sum);
                }
            }
        }
        
        // Ordenar por valor de ventas descendente
        List<Map.Entry<String, Double>> sortedSales = salesValueByProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        // Calcular total de ventas
        double totalSales = sortedSales.stream().mapToDouble(Map.Entry::getValue).sum();
        
        // Clasificar en A, B, C
        List<Map<String, Object>> classificationA = new ArrayList<>();
        List<Map<String, Object>> classificationB = new ArrayList<>();
        List<Map<String, Object>> classificationC = new ArrayList<>();
        
        double cumulativePercentage = 0;
        for (Map.Entry<String, Double> entry : sortedSales) {
            double percentage = (entry.getValue() / totalSales) * 100;
            cumulativePercentage += percentage;
            
            Product product = entry.getKey() != null ? productRepository.findById(entry.getKey()).orElse(null) : null;
            Map<String, Object> item = new HashMap<>();
            item.put("productId", entry.getKey());
            item.put("productName", product != null ? product.getDescription() : "N/A");
            item.put("salesValue", entry.getValue());
            item.put("percentage", percentage);
            item.put("cumulativePercentage", cumulativePercentage);
            
            if (cumulativePercentage <= 80) {
                item.put("classification", "A");
                classificationA.add(item);
            } else if (cumulativePercentage <= 95) {
                item.put("classification", "B");
                classificationB.add(item);
            } else {
                item.put("classification", "C");
                classificationC.add(item);
            }
        }
        
        report.put("classificationA", classificationA);
        report.put("classificationB", classificationB);
        report.put("classificationC", classificationC);
        report.put("totalSalesValue", totalSales);
        report.put("generatedAt", LocalDateTime.now());
        
        return report;
    }

    // ========== INTEGRACIÓN CON COMPRAS/VENTAS ==========

    @Override
    @Transactional
    public void registerPurchaseMovement(String purchaseInvoiceId, String productId, Double quantity,
                                          String presentationBarcode, String userId) {
        log.info("InventoryService -> registerPurchaseMovement for product: {}", productId);
        
        Product product = productRepository.findById(Objects.requireNonNull(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        Double previousStock = product.getStock().getQuantity().doubleValue();
        Double newStock = previousStock + quantity;
        
        // Actualizar stock
        product.getStock().setQuantity(BigDecimal.valueOf(newStock));
        productRepository.save(product);
        
        // Crear movimiento
        InventoryMovement movement = InventoryMovement.builder()
                .date(LocalDateTime.now())
                .productId(productId)
                .product(buildProductReference(product))
                .presentationBarcode(presentationBarcode)
                .movementType(EMovementType.COMPRA)
                .quantity(quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .unitMeasure(product.getStock().getUnitMeasure() != null ? 
                        product.getStock().getUnitMeasure().name() : null)
                .reference("COMPRA-" + purchaseInvoiceId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        
        movementRepository.save(movement);
    }

    @Override
    @Transactional
    public void registerSaleMovement(String billingId, String productId, Double quantity,
                                      String presentationBarcode, String userId) {
        log.info("InventoryService -> registerSaleMovement for product: {}", productId);
        
        Product product = productRepository.findById(Objects.requireNonNull(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        Double previousStock = product.getStock().getQuantity().doubleValue();
        Double newStock = previousStock - quantity;
        
        // Actualizar stock
        product.getStock().setQuantity(BigDecimal.valueOf(newStock));
        productRepository.save(product);
        
        // Crear movimiento
        InventoryMovement movement = InventoryMovement.builder()
                .date(LocalDateTime.now())
                .productId(productId)
                .product(buildProductReference(product))
                .presentationBarcode(presentationBarcode)
                .movementType(EMovementType.VENTA)
                .quantity(-quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .unitMeasure(product.getStock().getUnitMeasure() != null ? 
                        product.getStock().getUnitMeasure().name() : null)
                .reference("VENTA-" + billingId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        
        movementRepository.save(movement);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private ProductReference buildProductReference(Product product) {
        String barcode = "";
        if (product.getPresentations() != null && !product.getPresentations().isEmpty()) {
            barcode = product.getPresentations().get(0).getBarcode();
        }
        
        return ProductReference.builder()
                .id(product.getId())
                .description(product.getDescription())
                .barcode(barcode)
                .build();
    }

    private Double calculateNewStock(Double previousStock, Double quantity, EMovementType movementType) {
        switch (movementType) {
            case COMPRA:
            case DEVOLUCION_VENTA:
                return previousStock + quantity;
            case VENTA:
            case DEVOLUCION_COMPRA:
                return previousStock - Math.abs(quantity);
            case AJUSTE_FISICO:
            case AJUSTE_MANUAL:
                return previousStock + quantity; // quantity puede ser positiva o negativa
            default:
                return previousStock;
        }
    }
}
