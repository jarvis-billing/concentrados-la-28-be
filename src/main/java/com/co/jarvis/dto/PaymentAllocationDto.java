package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Monto de un pago que se aplica a una factura de compra.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocationDto implements Serializable {
    private String purchaseId;
    private BigDecimal amount;
}
