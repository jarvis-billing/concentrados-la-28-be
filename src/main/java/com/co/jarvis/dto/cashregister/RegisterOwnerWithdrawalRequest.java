package com.co.jarvis.dto.cashregister;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterOwnerWithdrawalRequest implements Serializable {

    /** Monto retirado por el propietario */
    private BigDecimal amount;

    /** Fecha del retiro (por defecto hoy) */
    private LocalDate date;

    /** Descripción libre opcional */
    private String description;

    /** Referencia opcional */
    private String reference;
}
