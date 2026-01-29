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
public class CreditSummary {

    private String clientId;
    private String clientName;
    private String clientIdNumber;
    private BigDecimal currentBalance;
    private BigDecimal totalDeposited;
    private BigDecimal totalUsed;
    private LocalDateTime lastTransactionDate;
}
