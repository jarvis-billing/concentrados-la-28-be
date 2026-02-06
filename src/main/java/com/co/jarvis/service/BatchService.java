package com.co.jarvis.service;

import com.co.jarvis.dto.batch.*;
import com.co.jarvis.entity.Batch;

import java.util.List;

public interface BatchService {

    List<Batch> getActiveBatchesByProductId(String productId);

    Batch getBatchById(String batchId);

    List<Batch> filterBatches(BatchFilter filter);

    Batch createBatch(CreateBatchRequest request);

    Batch updatePrice(UpdateBatchPriceRequest request);

    Batch registerSale(BatchSaleRequest request);

    List<BatchExpirationAlert> getExpiringSoonBatches();

    List<BatchSummary> getBatchSummary();

    Batch closeBatch(String batchId, CloseBatchRequest request);

    void checkExpiredBatches();

    void checkExpiringSoonBatches();
}
