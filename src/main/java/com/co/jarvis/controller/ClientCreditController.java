package com.co.jarvis.controller;

import com.co.jarvis.dto.AdjustCreditRequest;
import com.co.jarvis.dto.CreditReportFilter;
import com.co.jarvis.dto.CreditSummary;
import com.co.jarvis.dto.DepositCreditRequest;
import com.co.jarvis.dto.ManualCreditRequest;
import com.co.jarvis.dto.UseCreditRequest;
import com.co.jarvis.entity.ClientCredit;
import com.co.jarvis.entity.CreditTransaction;
import com.co.jarvis.service.ClientCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/client-credits", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClientCreditController {

    private final ClientCreditService clientCreditService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ClientCredit> getByClientId(@PathVariable String clientId) {
        log.info("ClientCreditController -> getByClientId: {}", clientId);
        ClientCredit credit = clientCreditService.getByClientId(clientId);
        if (credit == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(credit);
    }

    @GetMapping("/client/{clientId}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getClientCreditBalance(@PathVariable String clientId) {
        log.info("ClientCreditController -> getClientCreditBalance: {}", clientId);
        BigDecimal balance = clientCreditService.getClientCreditBalance(clientId);
        Map<String, BigDecimal> response = new HashMap<>();
        response.put("currentBalance", balance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}/transactions")
    public ResponseEntity<List<CreditTransaction>> getTransactionsByClientId(@PathVariable String clientId) {
        log.info("ClientCreditController -> getTransactionsByClientId: {}", clientId);
        List<CreditTransaction> transactions = clientCreditService.getTransactionsByClientId(clientId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/with-balance")
    public ResponseEntity<List<ClientCredit>> getAllWithBalance() {
        log.info("ClientCreditController -> getAllWithBalance");
        List<ClientCredit> credits = clientCreditService.getAllWithBalance();
        return ResponseEntity.ok(credits);
    }

    @PostMapping("/deposit")
    public ResponseEntity<CreditTransaction> registerDeposit(
            @RequestBody DepositCreditRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("ClientCreditController -> registerDeposit: clientId={}, amount={}", 
                request.getClientId(), request.getAmount());
        CreditTransaction transaction = clientCreditService.registerDeposit(request, userId);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/use")
    public ResponseEntity<CreditTransaction> useCredit(
            @RequestBody UseCreditRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("ClientCreditController -> useCredit: clientId={}, amount={}, billingId={}", 
                request.getClientId(), request.getAmount(), request.getBillingId());
        CreditTransaction transaction = clientCreditService.useCredit(request, userId);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/adjust")
    public ResponseEntity<CreditTransaction> adjustCredit(
            @RequestBody AdjustCreditRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("ClientCreditController -> adjustCredit: clientId={}, amount={}", 
                request.getClientId(), request.getAmount());
        CreditTransaction transaction = clientCreditService.adjustCredit(request, userId);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/report")
    public ResponseEntity<List<CreditSummary>> generateReport(@RequestBody CreditReportFilter filter) {
        log.info("ClientCreditController -> generateReport");
        List<CreditSummary> report = clientCreditService.generateReport(filter);
        return ResponseEntity.ok(report);
    }

    /**
     * POST /api/client-credits/manual
     * Registra un crédito manual para migrar datos del cuaderno físico al sistema.
     * Crea una transacción de tipo DEPOSIT con la fecha original del cuaderno.
     */
    @PostMapping("/manual")
    public ResponseEntity<?> registerManualCredit(
            @RequestBody ManualCreditRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("ClientCreditController -> registerManualCredit: clientId={}, amount={}, date={}", 
                request.getClientId(), request.getAmount(), request.getTransactionDate());
        
        try {
            CreditTransaction transaction = clientCreditService.registerManualCredit(request, userId);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            log.error("Error registering manual credit: {}", e.getMessage());
            if (e.getMessage().contains("Cliente no encontrado")) {
                return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
