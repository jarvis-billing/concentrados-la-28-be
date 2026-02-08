package com.co.jarvis.dto.cashregister;

import com.co.jarvis.enums.ECashCountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashCountSessionDto implements Serializable {
    private String id;
    private LocalDate sessionDate;
    private BigDecimal openingBalance;
    private List<CashDenominationDto> cashDenominations;
    private BigDecimal totalCashCounted;
    private BigDecimal expectedCashAmount;
    private BigDecimal expectedTransferAmount;
    private BigDecimal expectedOtherAmount;
    private BigDecimal cashDifference;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netCashFlow;
    private ECashCountStatus status;
    private String notes;
    private String cancelReason;
    private String createdBy;
    private LocalDateTime createdAt;
    private String closedBy;
    private LocalDateTime closedAt;
}
