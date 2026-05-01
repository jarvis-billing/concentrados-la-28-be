package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseInvoiceItem {
    private String productId;
    private String productCode;
    private String presentationBarcode;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    @Builder.Default
    private BigDecimal vatRate = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;
    @Builder.Default
    private Boolean applyFreight = false;
    @Builder.Default
    private BigDecimal totalWeightKg = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal freightAmount = BigDecimal.ZERO;

    private BigDecimal unitTotalCost;
}
