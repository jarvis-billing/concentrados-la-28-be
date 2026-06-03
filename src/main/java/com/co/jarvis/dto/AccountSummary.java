package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummary {

    private String clientId;
    private String clientName;
    private String clientIdNumber;
    private BigDecimal totalDebt;
    private BigDecimal totalPaid;
    private BigDecimal currentBalance;
    private LocalDateTime lastPaymentDate;
    private Long daysSinceLastPayment;

    /** Historial de pagos con saldo acumulado antes y después de cada abono */
    private List<PaymentWithBalance> payments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentWithBalance {
        private String id;
        private BigDecimal amount;
        private String paymentMethod;
        private String reference;
        private String notes;
        private LocalDateTime paymentDate;
        private String createdBy;
        /** Saldo pendiente ANTES de aplicar este pago */
        private BigDecimal balanceBefore;
        /** Saldo pendiente DESPUÉS de aplicar este pago */
        private BigDecimal balanceAfter;
    }
}
