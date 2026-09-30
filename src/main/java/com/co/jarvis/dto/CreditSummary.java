package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.co.jarvis.entity.CreditTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditSummary {

    private String clientId;
    private String clientName;
    private String clientIdNumber;
    private BigDecimal currentBalance;
    private BigDecimal totalDeposited;
    private BigDecimal totalUsed;
    private LocalDateTime lastTransactionDate;
    private List<CreditTransaction> transactions;

    // Datos del último depósito/abono (para mostrar en la fila principal del reporte)
    private LocalDateTime lastDepositDate;
    private String lastDepositMethod;
    private String lastDepositBankAccount;
}
