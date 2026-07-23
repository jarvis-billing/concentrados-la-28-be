package com.co.jarvis.service;

import com.co.jarvis.dto.inventorycount.BulkCountRequest;
import com.co.jarvis.dto.inventorycount.HideUncountedResultDto;
import com.co.jarvis.dto.inventorycount.InventoryCountReportDto;
import com.co.jarvis.dto.inventorycount.InventoryCountSessionDto;
import com.co.jarvis.dto.inventorycount.RecordCountRequest;
import com.co.jarvis.entity.InventoryCountSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryCountService {

    InventoryCountSession createSession(String username);

    InventoryCountSession getById(String sessionId);

    Optional<InventoryCountSession> findActiveSession();

    InventoryCountSession recordCount(String sessionId, RecordCountRequest request, String username);

    /**
     * Registra el conteo de múltiples presentaciones de un mismo producto en una sola llamada
     * y actualiza el stock del producto inmediatamente (stock = Σ qty_i × fixedAmount_i).
     */
    InventoryCountSession recordBulkCount(String sessionId, BulkCountRequest request, String username);

    InventoryCountSession pauseSession(String sessionId);

    InventoryCountSession completeSession(String sessionId, String username);

    InventoryCountReportDto getReport(String sessionId);

    List<InventoryCountSessionDto> listSessions(LocalDate fromDate, LocalDate toDate);

    /**
     * Marca active=false en todas las presentaciones que NO fueron contadas en la sesión.
     * Retorna el número de presentaciones ocultadas.
     */
    HideUncountedResultDto hideUncountedPresentations(String sessionId);
}
