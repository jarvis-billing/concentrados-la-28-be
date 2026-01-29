package com.co.jarvis.entity;

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
public class AccountPayment {

    private String id;
    private BigDecimal amount;
    private EPaymentMethod paymentMethod;
    private String reference;
    private String notes;
    private LocalDateTime paymentDate;
    private String createdBy;
    private LocalDateTime createdAt;
}
