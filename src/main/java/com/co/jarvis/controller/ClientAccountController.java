package com.co.jarvis.controller;

import com.co.jarvis.dto.AccountReportFilter;
import com.co.jarvis.dto.AccountSummary;
import com.co.jarvis.dto.BillingDto;
import com.co.jarvis.dto.ManualDebtRequest;
import com.co.jarvis.dto.PagedAccountReport;
import com.co.jarvis.dto.RegisterPaymentRequest;
import com.co.jarvis.util.reports.ReportExporter;
import net.sf.jasperreports.engine.JRException;
import com.co.jarvis.entity.AccountPayment;
import com.co.jarvis.entity.AccountTransaction;
import com.co.jarvis.entity.ClientAccount;
import com.co.jarvis.service.ClientAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/client-accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClientAccountController {

    private final ClientAccountService clientAccountService;
    private final ReportExporter reportExporter;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ClientAccount> getByClientId(@PathVariable String clientId) {
        log.info("ClientAccountController -> getByClientId: {}", clientId);
        ClientAccount account = clientAccountService.getByClientId(clientId);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    @GetMapping("/client/{clientId}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getClientBalance(@PathVariable String clientId) {
        log.info("ClientAccountController -> getClientBalance: {}", clientId);
        BigDecimal balance = clientAccountService.getClientBalance(clientId);
        Map<String, BigDecimal> response = new HashMap<>();
        response.put("currentBalance", balance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}/payments")
    public ResponseEntity<List<AccountPayment>> getPaymentsByClientId(@PathVariable String clientId) {
        log.info("ClientAccountController -> getPaymentsByClientId: {}", clientId);
        List<AccountPayment> payments = clientAccountService.getPaymentsByClientId(clientId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/client/{clientId}/credit-billings")
    public ResponseEntity<List<BillingDto>> getCreditBillingsByClientId(@PathVariable String clientId) {
        log.info("ClientAccountController -> getCreditBillingsByClientId: {}", clientId);
        List<BillingDto> billings = clientAccountService.getCreditBillingsByClientId(clientId);
        return ResponseEntity.ok(billings);
    }

    @GetMapping("/with-balance")
    public ResponseEntity<List<ClientAccount>> getAllWithBalance() {
        log.info("ClientAccountController -> getAllWithBalance");
        List<ClientAccount> accounts = clientAccountService.getAllWithBalance();
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/payments")
    public ResponseEntity<AccountPayment> registerPayment(
            @RequestBody RegisterPaymentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("ClientAccountController -> registerPayment: clientId={}, amount={}", 
                request.getClientAccountId(), request.getAmount());
        AccountPayment payment = clientAccountService.registerPayment(request, userId);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/report")
    public ResponseEntity<PagedAccountReport> generateReport(@RequestBody AccountReportFilter filter) {
        log.info("ClientAccountController -> generateReport");
        PagedAccountReport report = clientAccountService.generatePagedReport(filter);
        return ResponseEntity.ok(report);
    }

    @PostMapping(value = "/report/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateReportPdf(@RequestBody AccountReportFilter filter) {
        log.info("ClientAccountController -> generateReportPdf");
        try {
            // Obtener TODOS los registros (sin paginación) para el PDF
            AccountReportFilter allFilter = AccountReportFilter.builder()
                    .clientId(filter.getClientId())
                    .fromDate(filter.getFromDate())
                    .toDate(filter.getToDate())
                    .onlyWithBalance(filter.getOnlyWithBalance())
                    .page(0)
                    .size(Integer.MAX_VALUE)
                    .build();
            PagedAccountReport report = clientAccountService.generatePagedReport(allFilter);
            byte[] pdf = reportExporter.exportToPdf(report.getContent(), "accounts_receivable");
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"cuentas_por_cobrar.pdf\"")
                    .body(pdf);
        } catch (JRException | FileNotFoundException e) {
            log.error("Error generando PDF de cuentas por cobrar: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/manual-debt")
    public ResponseEntity<AccountTransaction> registerManualDebt(
            @RequestBody ManualDebtRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("ClientAccountController -> registerManualDebt: clientId={}, amount={}", 
                request.getClientId(), request.getAmount());
        try {
            AccountTransaction transaction = clientAccountService.registerManualDebt(request, userId);
            return ResponseEntity.ok(transaction);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for manual debt: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Cliente no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
}
