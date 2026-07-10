package com.co.jarvis.dto.inventorycount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCountSessionDto {

    private String id;
    private String sessionNumber;
    private String status;
    private String notes;
    private LocalDateTime startedAt;
    private String startedBy;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;
    private String completedBy;
    private List<InventoryCountEntryDto> entries;
    private int totalCounted;
}
