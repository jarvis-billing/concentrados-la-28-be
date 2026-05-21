package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchandiseReturnItem {
    private String productId;
    private String productCode;
    private String presentationBarcode;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    @Builder.Default
    private BigDecimal vatRate = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount;
}
