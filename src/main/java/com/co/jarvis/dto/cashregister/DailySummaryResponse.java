package com.co.jarvis.dto.cashregister;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryResponse implements Serializable {
    private List<CashTransactionDto> transactions;
    private List<PaymentMethodSummaryDto> paymentMethodSummaries;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal expectedCashAmount;
    private BigDecimal expectedTransferAmount;
    private BigDecimal expectedOtherAmount;
}
