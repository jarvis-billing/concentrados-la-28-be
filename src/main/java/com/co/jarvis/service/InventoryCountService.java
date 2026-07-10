package com.co.jarvis.service;

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

    InventoryCountSession pauseSession(String sessionId);

    InventoryCountSession completeSession(String sessionId, String username);

    InventoryCountReportDto getReport(String sessionId);

    List<InventoryCountSessionDto> listSessions(LocalDate fromDate, LocalDate toDate);
}
