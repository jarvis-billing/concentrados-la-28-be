package com.co.jarvis.controller;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.bankaccount.BankAccountDto;
import com.co.jarvis.dto.bankaccount.CreateBankAccountRequest;
import com.co.jarvis.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/bank-accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class BankAccountController {

    private final BankAccountService bankAccountService;

    /**
     * GET /api/bank-accounts
     * Lista las cuentas bancarias activas
     */
    @GetMapping
    public ResponseEntity<List<BankAccountDto>> listActive() {
        log.info("BankAccountController -> listActive");
        return ResponseEntity.ok(bankAccountService.listActive());
    }

    /**
     * GET /api/bank-accounts/all
     * Lista todas las cuentas bancarias (incluyendo inactivas)
     */
    @GetMapping("/all")
    public ResponseEntity<List<BankAccountDto>> listAll() {
        log.info("BankAccountController -> listAll");
        return ResponseEntity.ok(bankAccountService.listAll());
    }

    /**
     * GET /api/bank-accounts/{id}
     * Obtiene una cuenta bancaria por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        log.info("BankAccountController -> getById: {}", id);
        try {
            return ResponseEntity.ok(bankAccountService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/bank-accounts
     * Crea una nueva cuenta bancaria
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody CreateBankAccountRequest request) {
        log.info("BankAccountController -> create: name={}", request.getName());
        try {
            return ResponseEntity.ok(bankAccountService.create(request, getAuthenticatedUser()));
        } catch (RuntimeException e) {
            log.error("Error creating bank account: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/bank-accounts/{id}
     * Actualiza una cuenta bancaria existente
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CreateBankAccountRequest request) {
        log.info("BankAccountController -> update: {}", id);
        try {
            return ResponseEntity.ok(bankAccountService.update(id, request, getAuthenticatedUser()));
        } catch (RuntimeException e) {
            log.error("Error updating bank account: {}", e.getMessage());
            if (e.getMessage().contains("no encontrada")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/bank-accounts/{id}
     * Desactiva una cuenta bancaria (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(@PathVariable String id) {
        log.info("BankAccountController -> deactivate: {}", id);
        try {
            bankAccountService.deactivate(id, getAuthenticatedUser());
            return ResponseEntity.ok(Map.of("message", "Cuenta bancaria desactivada"));
        } catch (RuntimeException e) {
            log.error("Error deactivating bank account: {}", e.getMessage());
            if (e.getMessage().contains("no encontrada")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private UserDto getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDto user) {
            return user;
        }
        return null;
    }
}
