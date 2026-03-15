package com.co.jarvis.controller;

import com.co.jarvis.dto.cashregister.CashLoanDto;
import com.co.jarvis.dto.cashregister.CreateCashLoanRequest;
import com.co.jarvis.dto.cashregister.ReturnCashLoanRequest;
import com.co.jarvis.enums.ECashLoanStatus;
import com.co.jarvis.service.CashLoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
@RequestMapping(value = "/api/cash-loans", produces = MediaType.APPLICATION_JSON_VALUE)
public class CashLoanController {

    private final CashLoanService cashLoanService;

    /**
     * POST /api/cash-loans
     * Registra un nuevo préstamo de caja
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody CreateCashLoanRequest request) {
        log.info("CashLoanController -> create: borrower={}", request.getBorrower());
        try {
            String userId = getAuthenticatedUserId();
            if (request.getLoanDate() == null) {
                request.setLoanDate(LocalDate.now());
            }
            CashLoanDto result = cashLoanService.create(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            log.error("Error creating cash loan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/cash-loans/{id}/return
     * Registra la devolución de un préstamo
     */
    @PostMapping(value = "/{id}/return", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerReturn(
            @PathVariable String id,
            @RequestBody ReturnCashLoanRequest request) {
        log.info("CashLoanController -> registerReturn: id={}", id);
        try {
            String userId = getAuthenticatedUserId();
            if (request.getReturnDate() == null) {
                request.setReturnDate(LocalDate.now());
            }
            CashLoanDto result = cashLoanService.registerReturn(id, request, userId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error registering return for loan {}: {}", id, e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/cash-loans/{id}/cancel
     * Anula un préstamo
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id) {
        log.info("CashLoanController -> cancel: id={}", id);
        try {
            String userId = getAuthenticatedUserId();
            CashLoanDto result = cashLoanService.cancel(id, userId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error cancelling loan {}: {}", id, e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/cash-loans/{id}
     * Obtiene un préstamo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        log.info("CashLoanController -> getById: {}", id);
        try {
            CashLoanDto result = cashLoanService.getById(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/cash-loans?fromDate=2026-03-01&toDate=2026-03-31&status=PENDIENTE
     * Lista préstamos con filtros opcionales
     */
    @GetMapping
    public ResponseEntity<List<CashLoanDto>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ECashLoanStatus status) {
        log.info("CashLoanController -> list: from={}, to={}, status={}", fromDate, toDate, status);
        List<CashLoanDto> result = cashLoanService.list(fromDate, toDate, status);
        return ResponseEntity.ok(result);
    }

    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return null;
    }
}
