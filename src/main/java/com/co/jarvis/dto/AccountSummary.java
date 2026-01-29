package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
