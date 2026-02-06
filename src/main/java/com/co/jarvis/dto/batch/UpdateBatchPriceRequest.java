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
public class UpdateBatchPriceRequest {
    private String productId;
    private BigDecimal newSalePrice;
    private Integer priceValidityDays;
    private String notes;
}
