package com.co.jarvis.entity;

import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.enums.ETransactionCategory;
import com.co.jarvis.enums.ETransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una transacción del día para el arqueo de caja
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashTransaction {
    
    private String id;
    private ETransactionType type;              // INGRESO o EGRESO
    private ETransactionCategory category;      // VENTA, PAGO_CREDITO, GASTO, etc.
    private String description;                 // Descripción de la transacción
    private BigDecimal amount;                  // Monto de la transacción
    private EPaymentMethod paymentMethod;       // Método de pago
    private String reference;                   // Referencia (para transferencias, etc.)
    private LocalDateTime transactionDate;      // Fecha y hora de la transacción
    private String relatedDocumentId;           // ID del documento relacionado (factura, gasto, etc.)
}
