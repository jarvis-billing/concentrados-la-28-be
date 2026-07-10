package com.co.jarvis.dto.inventorycount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordCountRequest {

    private String barcode;
    private String productId;
    private String description;
    private String presentationLabel;
    private BigDecimal countedQty;
}
