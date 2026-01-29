package com.co.jarvis.entity;

import com.co.jarvis.enums.ECreditTransactionType;
import com.co.jarvis.enums.EPaymentMethod;
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
public class CreditTransaction {

    private String id;
    private ECreditTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private EPaymentMethod paymentMethod;
    private String reference;
    private String billingId;
    private String notes;
    private LocalDateTime transactionDate;
    private String createdBy;
    private LocalDateTime createdAt;
}
