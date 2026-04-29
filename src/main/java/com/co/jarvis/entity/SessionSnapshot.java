package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Snapshot (foto) del estado de una sesión ANTES de ser reabierta.
 * Se almacena como lista embebida en CashCountSession y BankReconciliationSession.
 * Permite auditar cuántas veces y en qué condiciones se reabrió un cierre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSnapshot {

    private LocalDateTime snapshotAt;

    private String userId;

    private String userName;

    /** totalCashCounted (caja) o totalBankCounted (banco) al momento del cierre previo */
    private BigDecimal totalCounted;

    /** expectedCashTotal (caja) o expectedBankTotal (banco) al momento del cierre previo */
    private BigDecimal expectedTotal;

    /** cashDifference (caja) o difference (banco) al momento del cierre previo */
    private BigDecimal difference;

    /** Motivo de la reapertura */
    private String reason;
}
