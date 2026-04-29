package com.co.jarvis.service;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.cashregister.*;
import com.co.jarvis.enums.ECashCountStatus;

import java.time.LocalDate;
import java.util.List;

public interface BankReconciliationService {

    /**
     * Obtiene el resumen diario de transacciones bancarias (no-efectivo) para una fecha y cuenta bancaria
     */
    DailyBankSummaryResponse getDailySummary(LocalDate date, String bankAccountId);

    /**
     * Crea o actualiza una conciliación bancaria (el bankAccountId viene dentro del request)
     */
    BankReconciliationDto createOrUpdate(CreateBankReconciliationRequest request, UserDto user);

    /**
     * Obtiene una conciliación por fecha y cuenta bancaria
     */
    BankReconciliationDto getByDate(LocalDate date, String bankAccountId);

    /**
     * Obtiene una conciliación por ID
     */
    BankReconciliationDto getById(String id);

    /**
     * Cierra una conciliación bancaria
     */
    BankReconciliationDto close(String id, CloseBankReconciliationRequest request, UserDto user);

    /**
     * Anula una conciliación bancaria
     */
    BankReconciliationDto cancel(String id, CancelBankReconciliationRequest request, UserDto user);

    /**
     * Reabre una conciliación CERRADA. Guarda snapshot del estado previo al cierre.
     */
    BankReconciliationDto reopen(String id, String reason, UserDto user);

    /**
     * Lista conciliaciones con filtros opcionales.
     * Si bankAccountId es null, devuelve todas las cuentas.
     */
    List<BankReconciliationSummaryDto> list(LocalDate fromDate, LocalDate toDate, ECashCountStatus status, String bankAccountId);

    /**
     * Obtiene el saldo de apertura sugerido para una cuenta bancaria específica
     */
    SuggestedOpeningResponse getSuggestedOpening(String bankAccountId);
}
