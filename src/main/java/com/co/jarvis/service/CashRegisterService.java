package com.co.jarvis.service;

import com.co.jarvis.dto.cashregister.*;
import com.co.jarvis.enums.ECashCountStatus;

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
    CashCountSessionDto createOrUpdate(CreateCashCountRequest request, String createdBy);

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
    CashCountSessionDto close(String id, CloseCashCountRequest request, String closedBy);

    /**
     * Anula un arqueo de caja
     */
    CashCountSessionDto cancel(String id, CancelCashCountRequest request, String cancelledBy);

    /**
     * Lista arqueos con filtros opcionales
     */
    List<CashCountSummaryDto> list(LocalDate fromDate, LocalDate toDate, ECashCountStatus status);

    /**
     * Obtiene el saldo de apertura sugerido (efectivo contado del último arqueo cerrado)
     */
    SuggestedOpeningResponse getSuggestedOpening();
}
