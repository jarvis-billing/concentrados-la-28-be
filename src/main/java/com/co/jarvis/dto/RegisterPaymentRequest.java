package com.co.jarvis.dto;

import com.co.jarvis.enums.EPaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPaymentRequest {

    private String clientAccountId;
    private BigDecimal amount;
    private EPaymentMethod paymentMethod;
    private String reference;
    private String notes;
}
