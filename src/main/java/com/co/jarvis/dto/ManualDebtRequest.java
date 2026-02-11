package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para registrar una deuda manual (migración de datos del cuaderno físico)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualDebtRequest {

    private String clientId;           // ID del cliente (requerido)
    private BigDecimal amount;         // Monto de la deuda (requerido, > 0)
    private LocalDate transactionDate; // Fecha original del cuaderno (requerido)
    private String notes;              // Descripción (requerido)
    
    @Builder.Default
    private String source = "MIGRACION_CUADERNO"; // Origen de la deuda (opcional, default: MIGRACION_CUADERNO)
}
