package com.co.jarvis.repository;

import com.co.jarvis.entity.PhysicalInventory;
import com.co.jarvis.enums.EAdjustmentReason;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PhysicalInventoryRepository extends MongoRepository<PhysicalInventory, String> {

    List<PhysicalInventory> findByProductId(String productId);

    List<PhysicalInventory> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<PhysicalInventory> findByAdjustmentReason(EAdjustmentReason reason);

    List<PhysicalInventory> findByProductIdAndDateBetween(
            String productId, LocalDateTime startDate, LocalDateTime endDate
    );

    List<PhysicalInventory> findAllByOrderByDateDesc();
}
