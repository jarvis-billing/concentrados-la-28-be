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
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
}
