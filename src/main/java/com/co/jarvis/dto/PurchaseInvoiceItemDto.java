package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseInvoiceItemDto implements Serializable {
    private String productId;
    private String productCode;
    private String presentationBarcode;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
}
