package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para registrar un crédito manual (migración de datos del cuaderno físico)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualCreditRequest {

    private String clientId;           // ID del cliente (requerido)
    private BigDecimal amount;         // Monto del crédito (requerido, > 0)
    private LocalDate transactionDate; // Fecha original del cuaderno (requerido)
    private String notes;              // Descripción (requerido)
    
    @Builder.Default
    private String source = "MIGRACION_CUADERNO"; // Origen del crédito (opcional, default: MIGRACION_CUADERNO)
}
