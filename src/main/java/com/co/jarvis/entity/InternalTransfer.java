package com.co.jarvis.entity;

import com.co.jarvis.enums.EBankAccount;
import com.co.jarvis.enums.EInternalTransferStatus;
import com.co.jarvis.enums.EInternalTransferType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents an internal movement of funds between company accounts.
 * Currently supports moving cash from the physical register to a bank account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "INTERNAL_TRANSFERS")
public class InternalTransfer {

    @Id
    private String id;

    @Field("transfer_date")
    private LocalDate transferDate;

    @Field("transfer_date_time")
    private LocalDateTime transferDateTime;

    private BigDecimal amount;

    @Builder.Default
    private EInternalTransferType type = EInternalTransferType.TRASLADO_EFECTIVO_BANCO;

    /**
     * Identifier of the source (e.g. "MAIN_CASH_REGISTER").
     * Kept as a free String because the system currently does not model
     * multiple cash registers as persistent entities.
     */
    @Field("source_id")
    private String sourceId;

    @Field("destination_bank_account_id")
    private String destinationBankAccountId;

    @Field("destination_bank_name")
    private String destinationBankName;

    @Field("destination_account_number")
    private String destinationAccountNumber;

    @Field("destination_account_type")
    private EBankAccount destinationAccountType;

    @Field("responsible_user_id")
    private String responsibleUserId;

    @Field("responsible_user_name")
    private String responsibleUserName;

    /**
     * Voucher / bank deposit slip reference provided by the user.
     */
    private String reference;

    private String notes;

    @Builder.Default
    private EInternalTransferStatus status = EInternalTransferStatus.ACTIVO;

    @Field("cancelled_at")
    private LocalDateTime cancelledAt;

    @Field("cancel_reason")
    private String cancelReason;

    @Field("created_at")
    private LocalDateTime createdAt;

    /**
     * Efectivo que el sistema "creía" tener al momento del registro.
     * Solo informativo — no se valida ni se muestra al usuario. Permite detectar
     * desfases entre capturas en papel y movimientos reales (p.ej. una consignación
     * mayor al saldo calculado evidencia ventas pendientes de capturar).
     */
    @Field("system_cash_snapshot")
    private BigDecimal systemCashSnapshot;

    @Field("snapshot_calculated_at")
    private LocalDateTime snapshotCalculatedAt;
}
