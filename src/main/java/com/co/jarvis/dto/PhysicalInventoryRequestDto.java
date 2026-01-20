package com.co.jarvis.dto;

import com.co.jarvis.enums.EAdjustmentReason;
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
public class PhysicalInventoryRequestDto {
    
    private String productId;
    
    private LocalDateTime date;
    
    private List<PresentationCountDto> presentationCounts;
    
    private Double physicalStock;
    
    private EAdjustmentReason adjustmentReason;
    
    private String notes;
    
    private String userId;
}
