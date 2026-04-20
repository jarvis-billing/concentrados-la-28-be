package com.co.jarvis.util.exception;

import java.math.BigDecimal;

/**
 * Thrown when an operation attempts to withdraw or move more funds
 * than are currently available in the source account (e.g. the cash register).
 */
public class InsufficientFundsException extends RuntimeException {

    private final BigDecimal requestedAmount;
    private final BigDecimal availableAmount;

    public InsufficientFundsException(String message) {
        super(message);
        this.requestedAmount = null;
        this.availableAmount = null;
    }

    public InsufficientFundsException(BigDecimal requestedAmount, BigDecimal availableAmount) {
        super(String.format(
                "Insufficient funds. Requested: %s, available: %s",
                requestedAmount, availableAmount));
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }
}
