package com.co.jarvis.dto.batch;

import com.co.jarvis.enums.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BatchFilter {
    private String productId;
    private BatchStatus status;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean onlyActive;
    private Boolean onlyExpiringSoon;
}
