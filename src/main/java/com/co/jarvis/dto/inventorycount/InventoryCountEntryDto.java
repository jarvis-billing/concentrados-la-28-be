package com.co.jarvis.dto.inventorycount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCountEntryDto {

    private String barcode;
    private String productId;
    private String description;
    private String presentationLabel;
    private BigDecimal systemStock;
    private BigDecimal countedQty;
    private BigDecimal difference;
    private LocalDateTime countedAt;
    private String countedBy;
}
