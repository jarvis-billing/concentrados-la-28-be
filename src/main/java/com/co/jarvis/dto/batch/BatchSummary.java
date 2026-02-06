package com.co.jarvis.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BatchSummary {
    private String productId;
    private String productDescription;
    private Integer activeBatches;
    private Integer totalStock;
    private LocalDate oldestBatchDate;
    private LocalDate newestBatchDate;
    private PriceRange priceRange;

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class PriceRange {
        private BigDecimal min;
        private BigDecimal max;
    }
}
