package com.co.jarvis.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class ProductSalesSummary {
    private String id; // barcode
    private String description;
    private BigDecimal totalAmount;
    private BigDecimal unitPrice;
    private BigDecimal totalSubTotal;
    private BigDecimal totalVat;
}

