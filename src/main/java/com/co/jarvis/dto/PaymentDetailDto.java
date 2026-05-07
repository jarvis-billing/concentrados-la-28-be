package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailDto implements Serializable {
    private String paymentId;
    private BigDecimal appliedAmount;
    private String paymentDate;
    private String method;
    private String reference;
    private String bankAccountName;
    private BigDecimal originalAmount;
}
