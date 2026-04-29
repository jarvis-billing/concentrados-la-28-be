package com.co.jarvis.entity;

import com.co.jarvis.enums.ECashCountStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una sesión de conciliación bancaria (arqueo de medios no-efectivo).
 * Cada registro compara el saldo bancario real (según extracto/app) contra
 * lo que el sistema calcula que debería haber en banco por transacciones
 * de transferencia, tarjeta, cheque, saldo a favor y traslados de caja.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "BANK_RECONCILIATION_SESSIONS")
@CompoundIndex(def = "{'sessionDate': 1, 'bankAccountId': 1}", unique = true, name = "idx_sessionDate_bankAccountId")
public class BankReconciliationSession {

    @Id
    private String id;

    private LocalDate sessionDate;

    @Field("bank_account_id")
    private String bankAccountId;

    @Field("bank_account_name")
    private String bankAccountName;

    private BigDecimal openingBalance;

    private BigDecimal totalBankCounted;

    private BigDecimal expectedBankAmount;

    private BigDecimal expectedBankTotal;

    private BigDecimal difference;

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private BigDecimal totalTransfers;

    private BigDecimal netBankFlow;

    @Builder.Default
    private ECashCountStatus status = ECashCountStatus.EN_PROGRESO;

    private String notes;

    private String cancelReason;

    @Builder.Default
    private List<AuditEntry> auditTrail = new ArrayList<>();

    @Builder.Default
    private List<SessionSnapshot> snapshots = new ArrayList<>();
}
