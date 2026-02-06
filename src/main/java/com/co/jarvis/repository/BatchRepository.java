package com.co.jarvis.repository;

import com.co.jarvis.entity.Batch;
import com.co.jarvis.enums.BatchStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BatchRepository extends MongoRepository<Batch, String> {

    List<Batch> findByProductIdAndStatus(String productId, BatchStatus status);

    List<Batch> findByProductIdAndStatusAndCurrentStockGreaterThan(String productId, BatchStatus status, Integer minStock);

    List<Batch> findByProductId(String productId);

    List<Batch> findByStatus(BatchStatus status);

    @Query("{ 'product_id': ?0, 'batch_number': { $exists: true } }")
    List<Batch> findByProductIdOrderByBatchNumberDesc(String productId);

    Optional<Batch> findTopByProductIdOrderByBatchNumberDesc(String productId);

    @Query("{ 'status': 'ACTIVE', 'expiration_date': { $lte: ?0 } }")
    List<Batch> findActiveExpiringSoon(LocalDate expirationThreshold);

    @Query("{ 'status': 'ACTIVE', 'expiration_date': { $lt: ?0 } }")
    List<Batch> findActiveExpired(LocalDate currentDate);

    @Query("{ 'product_id': ?0, 'current_stock': { $gt: 0 } }")
    List<Batch> findByProductIdWithStock(String productId);

    @Query(value = "{ 'status': 'ACTIVE' }", fields = "{ 'product_id': 1 }")
    List<Batch> findDistinctProductIdsWithActiveBatches();

    List<Batch> findByProductIdAndStatusIn(String productId, List<BatchStatus> statuses);

    @Query("{ 'entry_date': { $gte: ?0, $lte: ?1 } }")
    List<Batch> findByEntryDateBetween(LocalDate fromDate, LocalDate toDate);

    @Query("{ 'product_id': ?0, 'entry_date': { $gte: ?1, $lte: ?2 } }")
    List<Batch> findByProductIdAndEntryDateBetween(String productId, LocalDate fromDate, LocalDate toDate);

    long countByProductIdAndStatus(String productId, BatchStatus status);

    @Query("{ 'product_id': ?0, 'status': 'ACTIVE', 'current_stock': { $gt: 0 } }")
    List<Batch> findActiveBatchesWithStockByProductId(String productId);
}
