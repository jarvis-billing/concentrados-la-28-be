package com.co.jarvis.dto.cashregister;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen diario de transacciones bancarias (no-efectivo) para conciliación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBankSummaryResponse implements Serializable {

    private List<CashTransactionDto> transactions;
    private List<PaymentMethodSummaryDto> paymentMethodSummaries;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalTransfers;
    private BigDecimal openingBalance;
    private BigDecimal expectedBankAmount;
    private BigDecimal expectedBankTotal;
}
