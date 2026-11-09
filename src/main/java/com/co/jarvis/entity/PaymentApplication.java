package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Aplicación de un pago a proveedor sobre una factura de compra.
 * Un mismo pago puede aplicarse a varias facturas; lo no aplicado queda como anticipo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApplication {
    private String purchaseId;
    private String invoiceNumber;
    private BigDecimal amount;
    private OffsetDateTime appliedAt;
    private String appliedBy;

    /** Solo lectura (no se persiste): estado de pago actual de la factura, para reportes */
    @org.springframework.data.annotation.Transient
    private String invoicePaymentStatus;
}
