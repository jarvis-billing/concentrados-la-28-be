package com.co.jarvis.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BatchSaleRequest {
    private String batchId;
    private Integer quantity;
    private String billingId;
}
