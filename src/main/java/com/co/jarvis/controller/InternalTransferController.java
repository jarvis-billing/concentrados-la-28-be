package com.co.jarvis.controller;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.transfer.CashToBankTransferRequest;
import com.co.jarvis.dto.transfer.InternalTransferDto;
import com.co.jarvis.enums.EInternalTransferStatus;
import com.co.jarvis.enums.EInternalTransferType;
import com.co.jarvis.service.InternalTransferService;
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
@RequestMapping(value = "/api/v1/transfers", produces = MediaType.APPLICATION_JSON_VALUE)
public class InternalTransferController {

    private final InternalTransferService internalTransferService;

    /**
     * POST /api/v1/transfers/cash-to-bank
     * Registra una consignación de efectivo desde la caja hacia una cuenta bancaria.
     */
    @PostMapping(value = "/cash-to-bank", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InternalTransferDto> transferCashToBank(
            @RequestBody CashToBankTransferRequest request) {
        log.info("InternalTransferController -> transferCashToBank: amount={}",
                request != null ? request.getAmount() : null);
        UserDto user = getAuthenticatedUser();
        InternalTransferDto result = internalTransferService.transferCashToBank(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /api/v1/transfers/{id}
     * Obtiene un traslado por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InternalTransferDto> getById(@PathVariable String id) {
        log.info("InternalTransferController -> getById: {}", id);
        return ResponseEntity.ok(internalTransferService.getById(id));
    }

    /**
     * POST /api/v1/transfers/{id}/cancel
     * Anula un traslado previamente registrado.
     */
    @PostMapping(value = "/{id}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InternalTransferDto> cancel(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("InternalTransferController -> cancel: {}", id);
        String reason = body != null ? body.get("reason") : null;
        UserDto user = getAuthenticatedUser();
        InternalTransferDto result = internalTransferService.cancel(id, reason, user);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/transfers?fromDate=2026-04-01&toDate=2026-04-30&type=TRASLADO_EFECTIVO_BANCO&status=ACTIVO
     * Lista traslados con filtros opcionales.
     */
    @GetMapping
    public ResponseEntity<List<InternalTransferDto>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) EInternalTransferType type,
            @RequestParam(required = false) EInternalTransferStatus status) {
        log.info("InternalTransferController -> list: from={}, to={}, type={}, status={}",
                fromDate, toDate, type, status);
        return ResponseEntity.ok(internalTransferService.list(fromDate, toDate, type, status));
    }

    private UserDto getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDto user) {
            return user;
        }
        return null;
    }
}
