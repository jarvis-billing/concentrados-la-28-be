package com.co.jarvis.service.impl;

import com.co.jarvis.dto.batch.*;
import com.co.jarvis.entity.Batch;
import com.co.jarvis.entity.Product;
import com.co.jarvis.enums.BatchStatus;
import com.co.jarvis.repository.BatchRepository;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.service.BatchService;
import com.co.jarvis.util.constants.BatchConstants;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;

    @Override
    public List<Batch> getActiveBatchesByProductId(String productId) {
        log.info("BatchServiceImpl -> getActiveBatchesByProductId: {}", productId);
        return batchRepository.findByProductIdAndStatusAndCurrentStockGreaterThan(
                productId, BatchStatus.ACTIVE, 0);
    }

    @Override
    public Batch getBatchById(String batchId) {
        log.info("BatchServiceImpl -> getBatchById: {}", batchId);
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + batchId));
    }

    @Override
    public List<Batch> filterBatches(BatchFilter filter) {
        log.info("BatchServiceImpl -> filterBatches");
        
        List<Batch> batches;
        
        if (filter.getProductId() != null && !filter.getProductId().isBlank()) {
            if (filter.getStatus() != null) {
                batches = batchRepository.findByProductIdAndStatus(filter.getProductId(), filter.getStatus());
            } else {
                batches = batchRepository.findByProductId(filter.getProductId());
            }
        } else if (filter.getStatus() != null) {
            batches = batchRepository.findByStatus(filter.getStatus());
        } else {
            batches = batchRepository.findAll();
        }

        if (filter.getFromDate() != null && filter.getToDate() != null) {
            batches = batches.stream()
                    .filter(b -> !b.getEntryDate().isBefore(filter.getFromDate()) 
                            && !b.getEntryDate().isAfter(filter.getToDate()))
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(filter.getOnlyActive())) {
            batches = batches.stream()
                    .filter(b -> b.getStatus() == BatchStatus.ACTIVE && b.getCurrentStock() > 0)
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(filter.getOnlyExpiringSoon())) {
            LocalDate threshold = LocalDate.now(COLOMBIA_ZONE).plusDays(BatchConstants.BATCH_EXPIRATION_ALERT_DAYS);
            batches = batches.stream()
                    .filter(b -> b.getStatus() == BatchStatus.ACTIVE 
                            && b.getExpirationDate() != null 
                            && !b.getExpirationDate().isAfter(threshold))
                    .collect(Collectors.toList());
        }

        return batches;
    }

    @Override
    @Transactional
    public Batch createBatch(CreateBatchRequest request) {
        log.info("BatchServiceImpl -> createBatch for productId: {}", request.getProductId());

        validateProduct(request.getProductId());

        int batchNumber = calculateNextBatchNumber(request.getProductId());
        LocalDate entryDate = LocalDate.now(COLOMBIA_ZONE);
        int priceValidityDays = request.getPriceValidityDays() != null 
                ? request.getPriceValidityDays() 
                : BatchConstants.BATCH_DEFAULT_PRICE_VALIDITY_DAYS;
        LocalDate expirationDate = entryDate.plusDays(priceValidityDays);
        LocalDateTime now = LocalDateTime.now(COLOMBIA_ZONE);

        Batch batch = Batch.builder()
                .batchNumber(batchNumber)
                .productId(request.getProductId())
                .productDescription(request.getProductDescription())
                .entryDate(entryDate)
                .salePrice(request.getSalePrice())
                .initialStock(request.getInitialStock())
                .currentStock(request.getInitialStock())
                .unitMeasure(request.getUnitMeasure() != null ? request.getUnitMeasure() : "UNIDAD")
                .priceValidityDays(priceValidityDays)
                .expirationDate(expirationDate)
                .status(BatchStatus.ACTIVE)
                .purchaseInvoiceId(request.getPurchaseInvoiceId())
                .notes(request.getNotes())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Batch savedBatch = batchRepository.save(batch);
        log.info("Batch created successfully with ID: {} and batchNumber: {}", savedBatch.getId(), savedBatch.getBatchNumber());
        
        return savedBatch;
    }

    @Override
    @Transactional
    public Batch updatePrice(UpdateBatchPriceRequest request) {
        log.info("BatchServiceImpl -> updatePrice for productId: {}", request.getProductId());

        validateProduct(request.getProductId());

        List<Batch> activeBatches = batchRepository.findByProductIdAndStatus(
                request.getProductId(), BatchStatus.ACTIVE);

        if (activeBatches.isEmpty()) {
            throw new SaveRecordException("No hay lotes activos para actualizar el precio del producto: " + request.getProductId());
        }

        int totalStock = activeBatches.stream()
                .mapToInt(Batch::getCurrentStock)
                .sum();

        Integer priceValidityDays = request.getPriceValidityDays();
        if (priceValidityDays == null) {
            priceValidityDays = activeBatches.stream()
                    .filter(b -> b.getPriceValidityDays() != null)
                    .findFirst()
                    .map(Batch::getPriceValidityDays)
                    .orElse(BatchConstants.BATCH_DEFAULT_PRICE_VALIDITY_DAYS);
        }

        LocalDateTime now = LocalDateTime.now(COLOMBIA_ZONE);
        for (Batch batch : activeBatches) {
            batch.setStatus(BatchStatus.CLOSED);
            batch.setUpdatedAt(now);
            batch.setNotes(appendNote(batch.getNotes(), "Cerrado por actualización de precio"));
        }
        batchRepository.saveAll(activeBatches);
        log.info("Closed {} active batches for product: {}", activeBatches.size(), request.getProductId());

        int newBatchNumber = calculateNextBatchNumber(request.getProductId());
        LocalDate entryDate = LocalDate.now(COLOMBIA_ZONE);
        LocalDate expirationDate = entryDate.plusDays(priceValidityDays);

        Batch newBatch = Batch.builder()
                .batchNumber(newBatchNumber)
                .productId(request.getProductId())
                .entryDate(entryDate)
                .salePrice(request.getNewSalePrice())
                .initialStock(totalStock)
                .currentStock(totalStock)
                .unitMeasure("UNIDAD")
                .priceValidityDays(priceValidityDays)
                .expirationDate(expirationDate)
                .status(BatchStatus.ACTIVE)
                .notes(request.getNotes())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Batch savedBatch = batchRepository.save(newBatch);
        log.info("New batch created with updated price. ID: {}, batchNumber: {}, totalStock: {}", 
                savedBatch.getId(), savedBatch.getBatchNumber(), totalStock);

        return savedBatch;
    }

    @Override
    @Transactional
    public Batch registerSale(BatchSaleRequest request) {
        log.info("BatchServiceImpl -> registerSale for batchId: {}, quantity: {}", 
                request.getBatchId(), request.getQuantity());

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + request.getBatchId()));

        if (batch.getStatus() != BatchStatus.ACTIVE) {
            throw new SaveRecordException("El lote no está activo. Estado actual: " + batch.getStatus());
        }

        if (request.getQuantity() > batch.getCurrentStock()) {
            throw new SaveRecordException(String.format(
                    "Stock insuficiente. Solicitado: %d, Disponible: %d", 
                    request.getQuantity(), batch.getCurrentStock()));
        }

        batch.setCurrentStock(batch.getCurrentStock() - request.getQuantity());
        batch.setUpdatedAt(LocalDateTime.now(COLOMBIA_ZONE));

        if (batch.getCurrentStock() == 0) {
            batch.setStatus(BatchStatus.DEPLETED);
            log.info("Batch {} depleted after sale", batch.getId());
        }

        Batch savedBatch = batchRepository.save(batch);
        log.info("Sale registered. Batch: {}, Remaining stock: {}", savedBatch.getId(), savedBatch.getCurrentStock());

        return savedBatch;
    }

    @Override
    public List<BatchExpirationAlert> getExpiringSoonBatches() {
        log.info("BatchServiceImpl -> getExpiringSoonBatches");
        
        LocalDate today = LocalDate.now(COLOMBIA_ZONE);
        LocalDate threshold = today.plusDays(BatchConstants.BATCH_EXPIRATION_ALERT_DAYS);

        List<Batch> expiringSoonBatches = batchRepository.findActiveExpiringSoon(threshold);

        return expiringSoonBatches.stream()
                .map(batch -> {
                    int daysUntilExpiration = (int) java.time.temporal.ChronoUnit.DAYS.between(today, batch.getExpirationDate());
                    return BatchExpirationAlert.builder()
                            .batch(batch)
                            .daysUntilExpiration(daysUntilExpiration)
                            .requiresAction(daysUntilExpiration <= BatchConstants.BATCH_EXPIRATION_ALERT_DAYS)
                            .productDescription(batch.getProductDescription())
                            .build();
                })
                .sorted(Comparator.comparingInt(BatchExpirationAlert::getDaysUntilExpiration))
                .collect(Collectors.toList());
    }

    @Override
    public List<BatchSummary> getBatchSummary() {
        log.info("BatchServiceImpl -> getBatchSummary");

        List<Batch> activeBatches = batchRepository.findByStatus(BatchStatus.ACTIVE);

        Map<String, List<Batch>> batchesByProduct = activeBatches.stream()
                .collect(Collectors.groupingBy(Batch::getProductId));

        List<BatchSummary> summaries = new ArrayList<>();

        for (Map.Entry<String, List<Batch>> entry : batchesByProduct.entrySet()) {
            String productId = entry.getKey();
            List<Batch> productBatches = entry.getValue();

            String productDescription = productRepository.findById(productId)
                    .map(Product::getDescription)
                    .orElse("Producto no encontrado");

            int totalStock = productBatches.stream()
                    .mapToInt(Batch::getCurrentStock)
                    .sum();

            LocalDate oldestDate = productBatches.stream()
                    .map(Batch::getEntryDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            LocalDate newestDate = productBatches.stream()
                    .map(Batch::getEntryDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            BigDecimal minPrice = productBatches.stream()
                    .map(Batch::getSalePrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal maxPrice = productBatches.stream()
                    .map(Batch::getSalePrice)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BatchSummary summary = BatchSummary.builder()
                    .productId(productId)
                    .productDescription(productDescription)
                    .activeBatches(productBatches.size())
                    .totalStock(totalStock)
                    .oldestBatchDate(oldestDate)
                    .newestBatchDate(newestDate)
                    .priceRange(BatchSummary.PriceRange.builder()
                            .min(minPrice)
                            .max(maxPrice)
                            .build())
                    .build();

            summaries.add(summary);
        }

        return summaries;
    }

    @Override
    @Transactional
    public Batch closeBatch(String batchId, CloseBatchRequest request) {
        log.info("BatchServiceImpl -> closeBatch: {}", batchId);

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + batchId));

        batch.setStatus(BatchStatus.CLOSED);
        batch.setUpdatedAt(LocalDateTime.now(COLOMBIA_ZONE));

        if (request != null && request.getNotes() != null && !request.getNotes().isBlank()) {
            batch.setNotes(appendNote(batch.getNotes(), request.getNotes()));
        }

        Batch savedBatch = batchRepository.save(batch);
        log.info("Batch {} closed successfully", batchId);

        return savedBatch;
    }

    @Override
    @Transactional
    public void checkExpiredBatches() {
        log.info("BatchServiceImpl -> checkExpiredBatches (Scheduled Task)");
        
        LocalDate today = LocalDate.now(COLOMBIA_ZONE);
        List<Batch> expiredBatches = batchRepository.findActiveExpired(today);

        if (expiredBatches.isEmpty()) {
            log.info("No expired batches found");
            return;
        }

        LocalDateTime now = LocalDateTime.now(COLOMBIA_ZONE);
        for (Batch batch : expiredBatches) {
            batch.setStatus(BatchStatus.EXPIRED);
            batch.setUpdatedAt(now);
            batch.setNotes(appendNote(batch.getNotes(), "Expirado automáticamente el " + today));
        }

        batchRepository.saveAll(expiredBatches);
        log.info("Marked {} batches as EXPIRED", expiredBatches.size());
    }

    @Override
    public void checkExpiringSoonBatches() {
        log.info("BatchServiceImpl -> checkExpiringSoonBatches (Scheduled Task)");
        
        LocalDate threshold = LocalDate.now(COLOMBIA_ZONE).plusDays(BatchConstants.BATCH_EXPIRATION_ALERT_DAYS);
        List<Batch> expiringSoon = batchRepository.findActiveExpiringSoon(threshold);

        if (expiringSoon.isEmpty()) {
            log.info("No batches expiring soon");
            return;
        }

        for (Batch batch : expiringSoon) {
            log.warn("ALERT: Batch {} (Product: {}) expires on {}. Current stock: {}", 
                    batch.getBatchNumber(), batch.getProductId(), batch.getExpirationDate(), batch.getCurrentStock());
        }

        log.info("Found {} batches expiring within {} days", expiringSoon.size(), BatchConstants.BATCH_EXPIRATION_ALERT_DAYS);
    }

    private void validateProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + productId));

        if (!BatchConstants.BATCH_REQUIRED_CATEGORY.equalsIgnoreCase(product.getCategory())) {
            throw new SaveRecordException(String.format(
                    "El producto '%s' no pertenece a la categoría '%s'. Categoría actual: '%s'",
                    product.getDescription(), BatchConstants.BATCH_REQUIRED_CATEGORY, product.getCategory()));
        }
    }

    private int calculateNextBatchNumber(String productId) {
        List<Batch> allBatches = batchRepository.findByProductId(productId);

        if (allBatches.isEmpty()) {
            return 1;
        }

        boolean allDepleted = allBatches.stream()
                .allMatch(b -> b.getCurrentStock() == null || b.getCurrentStock() == 0);

        if (allDepleted) {
            log.info("All batches for product {} are depleted. Resetting batch number to 1", productId);
            return 1;
        }

        int maxBatchNumber = allBatches.stream()
                .mapToInt(b -> b.getBatchNumber() != null ? b.getBatchNumber() : 0)
                .max()
                .orElse(0);

        return maxBatchNumber + 1;
    }

    private String appendNote(String existingNotes, String newNote) {
        if (existingNotes == null || existingNotes.isBlank()) {
            return newNote;
        }
        return existingNotes + " | " + newNote;
    }
}
