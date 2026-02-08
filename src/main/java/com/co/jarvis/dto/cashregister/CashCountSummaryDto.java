package com.co.jarvis.dto.cashregister;

import com.co.jarvis.enums.ECashCountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para listado de arqueos (reportes)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashCountSummaryDto implements Serializable {
    private LocalDate date;
    private BigDecimal openingBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal expectedCash;
    private BigDecimal countedCash;
    private BigDecimal difference;
    private ECashCountStatus status;
    private String closedBy;
}
