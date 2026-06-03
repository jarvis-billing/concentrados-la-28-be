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
public class CloseCashCountRequest implements Serializable {
    private String notes;

    /**
     * Monto que el operador decide dejar en caja como fondo fijo para el día siguiente.
     * Configurable — no siempre es el mismo valor.
     * Si es null, no se aplica lógica de fondo fijo.
     */
    private BigDecimal closingBase;
}
