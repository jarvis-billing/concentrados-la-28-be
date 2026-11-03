package com.co.jarvis.dto.cashregister;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashCountConfigDto implements Serializable {
    private Boolean requireNoteOnDifference;
    private BigDecimal differenceThreshold;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
