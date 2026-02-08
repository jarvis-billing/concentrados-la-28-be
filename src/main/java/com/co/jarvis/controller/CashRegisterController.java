package com.co.jarvis.controller;

import com.co.jarvis.dto.cashregister.*;
import com.co.jarvis.enums.ECashCountStatus;
import com.co.jarvis.service.CashRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/cash-register", produces = MediaType.APPLICATION_JSON_VALUE)
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    /**
     * GET /api/cash-register/daily-summary?date=2026-02-07
     * Obtiene el resumen diario de transacciones para una fecha específica
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<DailySummaryResponse> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("CashRegisterController -> getDailySummary: {}", date);
        DailySummaryResponse response = cashRegisterService.getDailySummary(date);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cash-register
     * Crea o actualiza un arqueo de caja
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrUpdate(
            @RequestBody CreateCashCountRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("CashRegisterController -> createOrUpdate: date={}", request.getSessionDate());
        
        try {
            CashCountSessionDto session = cashRegisterService.createOrUpdate(request, userId);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            log.error("Error creating/updating cash count: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/cash-register/by-date?date=2026-02-07
     * Obtiene un arqueo por fecha
     */
    @GetMapping("/by-date")
    public ResponseEntity<CashCountSessionDto> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("CashRegisterController -> getByDate: {}", date);
        CashCountSessionDto session = cashRegisterService.getByDate(date);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * GET /api/cash-register/{id}
     * Obtiene un arqueo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CashCountSessionDto> getById(@PathVariable String id) {
        log.info("CashRegisterController -> getById: {}", id);
        CashCountSessionDto session = cashRegisterService.getById(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * POST /api/cash-register/{id}/close
     * Cierra un arqueo de caja
     */
    @PostMapping(value = "/{id}/close", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> close(
            @PathVariable String id,
            @RequestBody CloseCashCountRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("CashRegisterController -> close: {}", id);
        
        try {
            CashCountSessionDto session = cashRegisterService.close(id, request, userId);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            log.error("Error closing cash count: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/cash-register/{id}/cancel
     * Anula un arqueo de caja
     */
    @PostMapping(value = "/{id}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancel(
            @PathVariable String id,
            @RequestBody CancelCashCountRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("CashRegisterController -> cancel: {}", id);
        
        try {
            CashCountSessionDto session = cashRegisterService.cancel(id, request, userId);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            log.error("Error cancelling cash count: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/cash-register?fromDate=2026-01-01&toDate=2026-02-07&status=CERRADO
     * Lista arqueos con filtros opcionales
     */
    @GetMapping
    public ResponseEntity<List<CashCountSummaryDto>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ECashCountStatus status) {
        log.info("CashRegisterController -> list: from={}, to={}, status={}", fromDate, toDate, status);
        List<CashCountSummaryDto> sessions = cashRegisterService.list(fromDate, toDate, status);
        return ResponseEntity.ok(sessions);
    }

    /**
     * GET /api/cash-register/suggested-opening
     * Obtiene el saldo de apertura sugerido (efectivo contado del último arqueo cerrado)
     */
    @GetMapping("/suggested-opening")
    public ResponseEntity<SuggestedOpeningResponse> getSuggestedOpening() {
        log.info("CashRegisterController -> getSuggestedOpening");
        SuggestedOpeningResponse response = cashRegisterService.getSuggestedOpening();
        return ResponseEntity.ok(response);
    }
}
