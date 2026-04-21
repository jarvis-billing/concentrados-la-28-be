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
     * Bank account number of destination (required).
     */
    private String accountNumber;

    /**
     * Optional descriptive fields for the bank account (stored for auditing).
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
