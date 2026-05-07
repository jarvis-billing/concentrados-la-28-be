package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Clase embebida que representa un pago vinculado a una PurchaseInvoice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedPayment {
    private String paymentId;
    private BigDecimal appliedAmount;
    private String paymentDate;
    private String method;
    private String reference;
}
