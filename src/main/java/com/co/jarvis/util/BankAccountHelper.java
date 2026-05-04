package com.co.jarvis.util;

import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Helper para validar y enriquecer la información de cuenta bancaria
 * en cualquier feature que registre pagos por TRANSFERENCIA.
 */
@Component
public class BankAccountHelper {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    /**
     * Si el método es TRANSFERENCIA y no se envió bankAccountId, agrega un error al mapa.
     */
    public void validateTransfer(EPaymentMethod method, String bankAccountId, Map<String, String> errors, String fieldKey) {
        if (method == EPaymentMethod.TRANSFERENCIA && !StringUtils.hasText(bankAccountId)) {
            errors.put(fieldKey, "Requerido para pagos por transferencia");
        }
    }

    /**
     * Si llega un bankAccountId pero sin nombre, lo busca en BANK_ACCOUNTS y devuelve el bankName.
     * Si ya hay nombre o no hay id, devuelve el nombre actual sin cambios.
     */
    public String resolveBankAccountName(String bankAccountId, String currentName) {
        if (!StringUtils.hasText(bankAccountId)) {
            return currentName;
        }
        if (StringUtils.hasText(currentName)) {
            return currentName;
        }
        return bankAccountRepository.findById(bankAccountId)
                .map(ba -> ba.getBankName())
                .orElse(null);
    }

    /**
     * Lanza una excepción si el método es TRANSFERENCIA y no se envió bankAccountId.
     * Útil cuando el servicio no acumula errores en un mapa.
     */
    public void requireBankAccountForTransfer(EPaymentMethod method, String bankAccountId) {
        if (method == EPaymentMethod.TRANSFERENCIA && !StringUtils.hasText(bankAccountId)) {
            throw new IllegalArgumentException("bankAccountId: Requerido para pagos por transferencia");
        }
    }
}
