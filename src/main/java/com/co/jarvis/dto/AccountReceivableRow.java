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
public class AccountReceivableRow {
    private String clientId;
    private String clientName;
    private String clientNickname;
    private String clientPhone;
    private String clientAddress;
    private BigDecimal currentBalance;
    private LocalDateTime lastPaymentDate;
    private BigDecimal lastPaymentAmount;
}
