package com.co.jarvis.repository;

import com.co.jarvis.entity.InventoryCountSession;
import com.co.jarvis.enums.InventoryCountStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryCountSessionRepository extends MongoRepository<InventoryCountSession, String> {

    Optional<InventoryCountSession> findFirstByStatusOrderByStartedAtDesc(InventoryCountStatus status);

    List<InventoryCountSession> findByStatusInOrderByStartedAtDesc(List<InventoryCountStatus> statuses);
}
