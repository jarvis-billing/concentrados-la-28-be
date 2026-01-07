package com.co.jarvis.repository;

import com.co.jarvis.entity.InventoryMovement;
import com.co.jarvis.enums.EMovementType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends MongoRepository<InventoryMovement, String> {

    List<InventoryMovement> findByProductId(String productId);

    List<InventoryMovement> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<InventoryMovement> findByMovementType(EMovementType movementType);

    List<InventoryMovement> findByProductIdAndDateBetween(
            String productId, LocalDateTime startDate, LocalDateTime endDate
    );

    List<InventoryMovement> findByUserId(String userId);

    @Query("{ 'product_id': ?0, 'movement_type': ?1, 'date': { $gte: ?2, $lte: ?3 } }")
    List<InventoryMovement> findByFilters(
            String productId, EMovementType movementType,
            LocalDateTime startDate, LocalDateTime endDate
    );

    List<InventoryMovement> findByMovementTypeAndDateBetween(
            EMovementType movementType, LocalDateTime startDate, LocalDateTime endDate
    );

    List<InventoryMovement> findAllByOrderByDateDesc();
}
