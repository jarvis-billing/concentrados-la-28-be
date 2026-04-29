package com.co.jarvis.dto.cashregister;

import com.co.jarvis.enums.ECashCountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para listado de conciliaciones bancarias (reportes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankReconciliationSummaryDto implements Serializable {

    private LocalDate date;
    private String bankAccountId;
    private String bankAccountName;
    private BigDecimal openingBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal expectedBank;
    private BigDecimal countedBank;
    private BigDecimal difference;
    private ECashCountStatus status;
    private List<AuditEntryDto> auditTrail;
}
