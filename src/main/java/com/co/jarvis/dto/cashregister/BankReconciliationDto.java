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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankReconciliationDto implements Serializable {

    private String id;
    private LocalDate sessionDate;
    private String bankAccountId;
    private String bankAccountName;
    private BigDecimal openingBalance;
    private BigDecimal totalBankCounted;
    private BigDecimal expectedBankAmount;
    private BigDecimal expectedBankTotal;
    private BigDecimal difference;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalTransfers;
    private BigDecimal netBankFlow;
    private ECashCountStatus status;
    private String notes;
    private String cancelReason;
    private List<AuditEntryDto> auditTrail;
    private List<SessionSnapshotDto> snapshots;
}
