package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePaymentDetailResponse implements Serializable {
    private String purchaseId;
    private BigDecimal purchaseTotal;
    private BigDecimal totalPaid;
    private String paymentStatus;
    private List<PaymentDetailDto> payments;
}
