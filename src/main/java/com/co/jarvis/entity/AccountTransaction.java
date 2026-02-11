package com.co.jarvis.entity;

import com.co.jarvis.enums.EAccountTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransaction {

    private String id;
    private EAccountTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String billingId;
    private String notes;
    private String source;
    private LocalDate transactionDate;
    private String createdBy;
    private LocalDateTime createdAt;
}
