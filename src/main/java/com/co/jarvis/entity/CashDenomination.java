package com.co.jarvis.entity;

import com.co.jarvis.enums.EDenominationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Representa una denominación de efectivo (billete o moneda) con su cantidad contada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashDenomination {
    
    private Integer value;              // Valor de la denominación (100000, 50000, etc.)
    private EDenominationType type;     // BILLETE o MONEDA
    private Integer quantity;           // Cantidad contada
    private BigDecimal subtotal;        // value * quantity
}
