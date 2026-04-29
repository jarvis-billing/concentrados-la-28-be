package com.co.jarvis.dto.transfer;

import com.co.jarvis.enums.EBankAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to register a cash-to-bank deposit from the physical cash register.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashToBankTransferRequest implements Serializable {

    /**
     * Amount of cash being transferred. Must be positive.
     */
    private BigDecimal amount;

    /**
     * ID of an existing BankAccount entity (preferred).
     * If provided, bankName / accountNumber / accountType are populated automatically.
     * If null, the free-text fields below are used and a new BankAccount is created.
     */
    private String bankAccountId;

    /**
     * Human-readable alias for the account (only needed when creating a new one).
     * e.g. "Bancolombia Ahorros Principal".
     * Ignored when bankAccountId is provided.
     */
    private String accountName;

    /**
     * Bank account number of destination.
     * Required when bankAccountId is null (used to create/find the account).
     */
    private String accountNumber;

    /**
     * Optional descriptive fields (used when bankAccountId is null).
     */
    private String bankName;
    private EBankAccount accountType;

    /**
     * Voucher or deposit slip reference (required for auditing).
     */
    private String reference;

    /**
     * Optional: date the physical deposit happened. Defaults to today.
     */
    private LocalDate transferDate;

    private String notes;
}
