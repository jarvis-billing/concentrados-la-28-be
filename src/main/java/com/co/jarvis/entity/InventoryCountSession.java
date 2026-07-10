package com.co.jarvis.entity;

import com.co.jarvis.enums.InventoryCountStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sesión de conteo físico de inventario.
 * Registra qué productos fueron contados, en qué cantidad y por quién.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "INVENTORY_COUNT_SESSIONS")
@CompoundIndex(name = "status_startedAt_idx", def = "{'status': 1, 'startedAt': -1}")
public class InventoryCountSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionNumber;  // CONT-0001

    @Builder.Default
    private InventoryCountStatus status = InventoryCountStatus.IN_PROGRESS;

    private String notes;

    private LocalDateTime startedAt;
    private String startedBy;

    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;
    private String completedBy;

    @Builder.Default
    private List<InventoryCountEntry> entries = new ArrayList<>();
}
