package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDto implements Serializable {
    private String productId;
    private String productName;
    private String barcode;
    private Double currentStock;
    private String unitMeasure;
    private Double totalSold;
    private Double totalPurchased;
}
