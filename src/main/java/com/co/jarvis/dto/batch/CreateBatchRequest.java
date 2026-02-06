package com.co.jarvis.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateBatchRequest {
    private String productId;
    private BigDecimal salePrice;
    private Integer initialStock;
    private Integer priceValidityDays;
    private String unitMeasure;
    private String purchaseInvoiceId;
    private String notes;
}
