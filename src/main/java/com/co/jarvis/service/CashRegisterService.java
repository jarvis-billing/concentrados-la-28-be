package com.co.jarvis.service;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.cashregister.*;
import com.co.jarvis.enums.ECashCountStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CashRegisterService {

    /**
     * Obtiene el resumen diario de transacciones para una fecha específica
     */
    DailySummaryResponse getDailySummary(LocalDate date);

    /**
     * Crea o actualiza un arqueo de caja
     */
    CashCountSessionDto createOrUpdate(CreateCashCountRequest request, UserDto user);

    /**
     * Obtiene un arqueo por fecha
     */
    CashCountSessionDto getByDate(LocalDate date);

    /**
     * Obtiene un arqueo por ID
     */
    CashCountSessionDto getById(String id);

    /**
     * Cierra un arqueo de caja
     */
    CashCountSessionDto close(String id, CloseCashCountRequest request, UserDto user);

    /**
     * Anula un arqueo de caja
     */
    CashCountSessionDto cancel(String id, CancelCashCountRequest request, UserDto user);

    /**
     * Lista arqueos con filtros opcionales
     */
    List<CashCountSummaryDto> list(LocalDate fromDate, LocalDate toDate, ECashCountStatus status);

    /**
     * Obtiene el saldo de apertura sugerido (efectivo contado del último arqueo cerrado)
     */
    SuggestedOpeningResponse getSuggestedOpening();

    /**
     * Calcula el saldo de efectivo disponible en la caja para una fecha dada.
     * saldo = apertura + ingresos en efectivo - egresos en efectivo (todos del día)
     * Útil para validar si hay dinero suficiente para un traslado a banco.
     */
    BigDecimal getCurrentCashBalance(LocalDate date);
}
