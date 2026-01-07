package com.co.jarvis.repository;

import com.co.jarvis.entity.InventoryAdjustment;
import com.co.jarvis.enums.EAdjustmentReason;
import com.co.jarvis.enums.EAdjustmentType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryAdjustmentRepository extends MongoRepository<InventoryAdjustment, String> {

    List<InventoryAdjustment> findByProductId(String productId);

    List<InventoryAdjustment> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<InventoryAdjustment> findByAdjustmentType(EAdjustmentType adjustmentType);

    List<InventoryAdjustment> findByReason(EAdjustmentReason reason);

    List<InventoryAdjustment> findByRequiresAuthorizationTrue();

    List<InventoryAdjustment> findByProductIdAndDateBetween(
            String productId, LocalDateTime startDate, LocalDateTime endDate
    );

    List<InventoryAdjustment> findAllByOrderByDateDesc();
}
