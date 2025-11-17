package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntry {
    private String method;      // EPaymentMethod as String value
    private BigDecimal amount;  // decimal(18,2)
    private String reference;   // optional
}
