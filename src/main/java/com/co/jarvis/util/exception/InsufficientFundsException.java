package com.co.jarvis.util.exception;

import java.math.BigDecimal;

/**
 * Thrown when an operation attempts to withdraw or move more funds
 * than are currently available in the source account (e.g. the cash register).
 *
 * @deprecated Las consignaciones (cash-to-bank) ya no validan fondos suficientes
 * porque los ingresos se capturan a destiempo y el sistema no es fuente de verdad
 * del efectivo físico. Esta clase se mantiene temporalmente para no romper el
 * handler global mientras el frontend limpia referencias a INSUFFICIENT_FUNDS.
 * Será eliminada en una versión futura.
 */
@Deprecated(forRemoval = true)
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
