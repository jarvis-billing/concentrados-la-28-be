package com.co.jarvis.controller;

import com.co.jarvis.dto.batch.*;
import com.co.jarvis.entity.Batch;
import com.co.jarvis.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/batches", produces = MediaType.APPLICATION_JSON_VALUE)
public class BatchController {

    private final BatchService batchService;

    @GetMapping("/product/{productId}/active")
    public ResponseEntity<List<Batch>> getActiveBatchesByProductId(@PathVariable String productId) {
        log.info("BatchController -> getActiveBatchesByProductId: {}", productId);
        List<Batch> batches = batchService.getActiveBatchesByProductId(productId);
        return ResponseEntity.ok(batches);
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<Batch> getBatchById(@PathVariable String batchId) {
        log.info("BatchController -> getBatchById: {}", batchId);
        Batch batch = batchService.getBatchById(batchId);
        return ResponseEntity.ok(batch);
    }

    @PostMapping("/filter")
    public ResponseEntity<List<Batch>> filterBatches(@RequestBody BatchFilter filter) {
        log.info("BatchController -> filterBatches");
        List<Batch> batches = batchService.filterBatches(filter);
        return ResponseEntity.ok(batches);
    }

    @PostMapping
    public ResponseEntity<Batch> createBatch(@RequestBody CreateBatchRequest request) {
        log.info("BatchController -> createBatch for productId: {}", request.getProductId());
        Batch batch = batchService.createBatch(request);
        return ResponseEntity.ok(batch);
    }

    @PostMapping("/update-price")
    public ResponseEntity<Batch> updatePrice(@RequestBody UpdateBatchPriceRequest request) {
        log.info("BatchController -> updatePrice for productId: {}", request.getProductId());
        Batch batch = batchService.updatePrice(request);
        return ResponseEntity.ok(batch);
    }

    @PostMapping("/sale")
    public ResponseEntity<Batch> registerSale(@RequestBody BatchSaleRequest request) {
        log.info("BatchController -> registerSale for batchId: {}", request.getBatchId());
        Batch batch = batchService.registerSale(request);
        return ResponseEntity.ok(batch);
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<List<BatchExpirationAlert>> getExpiringSoonBatches() {
        log.info("BatchController -> getExpiringSoonBatches");
        List<BatchExpirationAlert> alerts = batchService.getExpiringSoonBatches();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/summary")
    public ResponseEntity<List<BatchSummary>> getBatchSummary() {
        log.info("BatchController -> getBatchSummary");
        List<BatchSummary> summaries = batchService.getBatchSummary();
        return ResponseEntity.ok(summaries);
    }

    @PostMapping("/{batchId}/close")
    public ResponseEntity<Batch> closeBatch(
            @PathVariable String batchId,
            @RequestBody(required = false) CloseBatchRequest request) {
        log.info("BatchController -> closeBatch: {}", batchId);
        Batch batch = batchService.closeBatch(batchId, request);
        return ResponseEntity.ok(batch);
    }
}
