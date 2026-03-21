package com.co.jarvis.entity;

import com.co.jarvis.enums.ECashCountStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una sesión de arqueo de caja
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "CASH_COUNT_SESSIONS")
public class CashCountSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private LocalDate sessionDate;              // Fecha del arqueo

    private BigDecimal openingBalance;          // Saldo de apertura

    @Builder.Default
    private List<CashDenomination> cashDenominations = new ArrayList<>();  // Denominaciones contadas

    private BigDecimal totalCashCounted;        // Total efectivo contado (suma de denominaciones)
    private BigDecimal expectedCashAmount;      // Neto efectivo del día (ingresos - egresos en efectivo)
    private BigDecimal expectedCashTotal;       // Efectivo total esperado en caja = openingBalance + expectedCashAmount
    private BigDecimal expectedTransferAmount;  // Transferencias esperadas
    private BigDecimal expectedOtherAmount;     // Otros métodos de pago esperados
    private BigDecimal cashDifference;          // Diferencia entre contado y esperado (totalCashCounted - expectedCashTotal)

    private BigDecimal totalIncome;             // Total ingresos del día
    private BigDecimal totalExpense;            // Total egresos del día
    private BigDecimal netCashFlow;             // Flujo neto de caja

    @Builder.Default
    private ECashCountStatus status = ECashCountStatus.EN_PROGRESO;  // Estado del arqueo

    private String notes;                       // Notas del arqueo
    private String cancelReason;                // Razón de anulación (si aplica)

    @Builder.Default
    private List<AuditEntry> auditTrail = new ArrayList<>();  // Historial de operaciones
}
