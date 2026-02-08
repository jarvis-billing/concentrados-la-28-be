package com.co.jarvis.dto.cashregister;

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
public class CashDenominationDto implements Serializable {
    private Integer value;
    private Integer quantity;
    private String type;        // BILLETE o MONEDA (calculado automáticamente)
    private BigDecimal subtotal; // Calculado: value * quantity
}
