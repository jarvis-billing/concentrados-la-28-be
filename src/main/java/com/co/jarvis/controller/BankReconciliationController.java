package com.co.jarvis.controller;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.cashregister.*;
import com.co.jarvis.enums.ECashCountStatus;
import com.co.jarvis.service.BankReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/bank-reconciliation", produces = MediaType.APPLICATION_JSON_VALUE)
public class BankReconciliationController {

    private final BankReconciliationService bankReconciliationService;

    /**
     * GET /api/bank-reconciliation/daily-summary?date=2026-02-07
     * Obtiene el resumen diario de transacciones bancarias (no-efectivo) para una fecha específica
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<DailyBankSummaryResponse> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String bankAccountId) {
        log.info("BankReconciliationController -> getDailySummary: date={}, bankAccountId={}", date, bankAccountId);
        DailyBankSummaryResponse response = bankReconciliationService.getDailySummary(date, bankAccountId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/bank-reconciliation
     * Crea o actualiza una conciliación bancaria
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrUpdate(@RequestBody CreateBankReconciliationRequest request) {
        log.info("BankReconciliationController -> createOrUpdate: date={}", request.getSessionDate());

        try {
            UserDto user = getAuthenticatedUser();
            BankReconciliationDto session = bankReconciliationService.createOrUpdate(request, user);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            log.error("Error creating/updating bank reconciliation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/bank-reconciliation/by-date?date=2026-02-07
     * Obtiene una conciliación por fecha
     */
    @GetMapping("/by-date")
    public ResponseEntity<BankReconciliationDto> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String bankAccountId) {
        log.info("BankReconciliationController -> getByDate: date={}, bankAccountId={}", date, bankAccountId);
        BankReconciliationDto session = bankReconciliationService.getByDate(date, bankAccountId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * GET /api/bank-reconciliation/{id}
     * Obtiene una conciliación por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BankReconciliationDto> getById(@PathVariable String id) {
        log.info("BankReconciliationController -> getById: {}", id);
        BankReconciliationDto session = bankReconciliationService.getById(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * POST /api/bank-reconciliation/{id}/close
     * Cierra una conciliación bancaria
     */
    @PostMapping(value = "/{id}/close", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> close(
            @PathVariable String id,
            @RequestBody CloseBankReconciliationRequest request) {
        log.info("BankReconciliationController -> close: {}", id);

        try {
            UserDto user = getAuthenticatedUser();
            BankReconciliationDto session = bankReconciliationService.close(id, request, user);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            log.error("Error closing bank reconciliation: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/bank-reconciliation/{id}/cancel
     * Anula una conciliación bancaria
     */
    @PostMapping(value = "/{id}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancel(
            @PathVariable String id,
            @RequestBody CancelBankReconciliationRequest request) {
        log.info("BankReconciliationController -> cancel: {}", id);

        try {
            UserDto user = getAuthenticatedUser();
            BankReconciliationDto session = bankReconciliationService.cancel(id, request, user);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            log.error("Error cancelling bank reconciliation: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/bank-reconciliation/{id}/reopen
     * Reabre una conciliación CERRADA. Guarda snapshot del estado previo al cierre.
     */
    @PostMapping(value = "/{id}/reopen", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reopen(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("BankReconciliationController -> reopen: {}", id);
        try {
            String reason = body != null ? body.get("reason") : null;
            UserDto user = getAuthenticatedUser();
            BankReconciliationDto session = bankReconciliationService.reopen(id, reason, user);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            log.error("Error reopening bank reconciliation: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/bank-reconciliation?fromDate=2026-01-01&toDate=2026-02-07&status=CERRADO
     * Lista conciliaciones con filtros opcionales
     */
    @GetMapping
    public ResponseEntity<List<BankReconciliationSummaryDto>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ECashCountStatus status,
            @RequestParam(required = false) String bankAccountId) {
        log.info("BankReconciliationController -> list: from={}, to={}, status={}, bankAccountId={}", fromDate, toDate, status, bankAccountId);
        List<BankReconciliationSummaryDto> sessions = bankReconciliationService.list(fromDate, toDate, status, bankAccountId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * GET /api/bank-reconciliation/suggested-opening
     * Obtiene el saldo de apertura sugerido (saldo bancario reportado del último cierre)
     */
    @GetMapping("/suggested-opening")
    public ResponseEntity<SuggestedOpeningResponse> getSuggestedOpening(
            @RequestParam String bankAccountId) {
        log.info("BankReconciliationController -> getSuggestedOpening: bankAccountId={}", bankAccountId);
        SuggestedOpeningResponse response = bankReconciliationService.getSuggestedOpening(bankAccountId);
        return ResponseEntity.ok(response);
    }

    private UserDto getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDto user) {
            return user;
        }
        return null;
    }
}
