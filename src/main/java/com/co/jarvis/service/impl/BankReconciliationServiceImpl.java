package com.co.jarvis.service.impl;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.cashregister.*;
import com.co.jarvis.entity.*;
import com.co.jarvis.enums.*;
import com.co.jarvis.repository.*;
import com.co.jarvis.service.BankReconciliationService;
import com.co.jarvis.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankReconciliationServiceImpl implements BankReconciliationService {

    private final BankReconciliationSessionRepository bankReconciliationSessionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final ClientCreditRepository clientCreditRepository;
    private final ClientAccountRepository clientAccountRepository;
    private final InternalTransferRepository internalTransferRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public DailyBankSummaryResponse getDailySummary(LocalDate date, String bankAccountId) {
        log.info("BankReconciliationServiceImpl -> getDailySummary: date={}, bankAccountId={}", date, bankAccountId);

        List<CashTransactionDto> transactions = new ArrayList<>();

        transactions.addAll(getSalesTransactions(date));
        transactions.addAll(getCreditPaymentTransactions(date));
        transactions.addAll(getDepositTransactions(date));
        transactions.addAll(getRefundTransactions(date));
        transactions.addAll(getExpenseTransactions(date));
        transactions.addAll(getSupplierPaymentTransactions(date));
        transactions.addAll(getInternalTransferTransactions(date, bankAccountId));

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == ETransactionType.INGRESO)
                .map(CashTransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() == ETransactionType.EGRESO)
                .map(CashTransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTransfers = getInternalTransferTransactions(date, bankAccountId).stream()
                .map(CashTransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedBankAmount = totalIncome.subtract(totalExpense);
        BigDecimal openingBalance = resolveOpeningBalance(date, bankAccountId);
        BigDecimal expectedBankTotal = openingBalance.add(expectedBankAmount);

        List<PaymentMethodSummaryDto> paymentMethodSummaries = calculatePaymentMethodSummaries(transactions);

        return DailyBankSummaryResponse.builder()
                .transactions(transactions)
                .paymentMethodSummaries(paymentMethodSummaries)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .totalTransfers(totalTransfers)
                .openingBalance(openingBalance)
                .expectedBankAmount(expectedBankAmount)
                .expectedBankTotal(expectedBankTotal)
                .build();
    }

    @Override
    @Transactional
    public BankReconciliationDto createOrUpdate(CreateBankReconciliationRequest request, UserDto user) {
        log.info("BankReconciliationServiceImpl -> createOrUpdate: date={}, user={}", request.getSessionDate(),
                user != null ? user.getNumberIdentity() : "unknown");

        String userId = user != null ? user.getNumberIdentity() : "unknown";
        String userName = user != null ? user.getFullName() : "unknown";

        String bankAccountId = request.getBankAccountId();
        if (bankAccountId == null || bankAccountId.isBlank()) {
            throw new RuntimeException("Se requiere el ID de cuenta bancaria (bankAccountId)");
        }

        String bankAccountName = bankAccountRepository.findById(bankAccountId)
                .map(a -> a.getName())
                .orElse(bankAccountId);

        Optional<BankReconciliationSession> existingSession = bankReconciliationSessionRepository
                .findBySessionDateAndBankAccountId(request.getSessionDate(), bankAccountId);

        BankReconciliationSession session;

        if (existingSession.isPresent()) {
            session = existingSession.get();

            if (session.getStatus() == ECashCountStatus.CERRADO) {
                throw new RuntimeException("La conciliación bancaria de esta fecha ya está cerrada y no puede modificarse");
            }
            if (session.getStatus() == ECashCountStatus.ANULADO) {
                throw new RuntimeException("La conciliación bancaria de esta fecha está anulada. Debe crear una nueva.");
            }

            session.setOpeningBalance(request.getOpeningBalance());
            session.setTotalBankCounted(request.getTotalBankCounted());
            session.setNotes(request.getNotes());
            ensureAuditTrail(session).add(AuditEntry.builder()
                    .userId(userId)
                    .userName(userName)
                    .action(EAuditAction.ACTUALIZACION)
                    .timestamp(LocalDateTime.now())
                    .build());
        } else {
            List<AuditEntry> auditTrail = new ArrayList<>();
            auditTrail.add(AuditEntry.builder()
                    .userId(userId)
                    .userName(userName)
                    .action(EAuditAction.APERTURA)
                    .timestamp(LocalDateTime.now())
                    .build());
            session = BankReconciliationSession.builder()
                    .sessionDate(request.getSessionDate())
                    .bankAccountId(bankAccountId)
                    .bankAccountName(bankAccountName)
                    .openingBalance(request.getOpeningBalance())
                    .totalBankCounted(request.getTotalBankCounted())
                    .notes(request.getNotes())
                    .status(ECashCountStatus.EN_PROGRESO)
                    .auditTrail(auditTrail)
                    .build();
        }

        DailyBankSummaryResponse dailySummary = getDailySummary(request.getSessionDate(), bankAccountId);

        session.setTotalIncome(dailySummary.getTotalIncome());
        session.setTotalExpense(dailySummary.getTotalExpense());
        session.setTotalTransfers(dailySummary.getTotalTransfers());
        session.setExpectedBankAmount(dailySummary.getExpectedBankAmount());

        BigDecimal expectedBankTotal = request.getOpeningBalance().add(dailySummary.getExpectedBankAmount());
        session.setExpectedBankTotal(expectedBankTotal);

        BigDecimal counted = request.getTotalBankCounted() != null ? request.getTotalBankCounted() : BigDecimal.ZERO;
        session.setDifference(counted.subtract(expectedBankTotal));

        session.setNetBankFlow(dailySummary.getTotalIncome().subtract(dailySummary.getTotalExpense()));

        log.info("=== DIAGNÓSTICO CONCILIACIÓN BANCARIA {} ===", request.getSessionDate());
        log.info("  openingBalance:     {}", request.getOpeningBalance());
        log.info("  bankIncome:         {}", dailySummary.getTotalIncome());
        log.info("  bankExpense:        {}", dailySummary.getTotalExpense());
        log.info("  transfersToBank:    {}", dailySummary.getTotalTransfers());
        log.info("  expectedBankAmount: {}", dailySummary.getExpectedBankAmount());
        log.info("  expectedBankTotal:  {}", expectedBankTotal);
        log.info("  countedBank:        {}", counted);
        log.info("  difference:         {}", counted.subtract(expectedBankTotal));
        log.info("  transacciones:      {}", dailySummary.getTransactions() != null ? dailySummary.getTransactions().size() : 0);
        log.info("=== FIN DIAGNÓSTICO ===");

        session = bankReconciliationSessionRepository.save(session);
        log.info("Bank reconciliation session saved with ID: {}", session.getId());

        return mapToDto(session);
    }

    @Override
    public BankReconciliationDto getByDate(LocalDate date, String bankAccountId) {
        log.info("BankReconciliationServiceImpl -> getByDate: date={}, bankAccountId={}", date, bankAccountId);
        return bankReconciliationSessionRepository.findBySessionDateAndBankAccountId(date, bankAccountId)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    public BankReconciliationDto getById(String id) {
        log.info("BankReconciliationServiceImpl -> getById: {}", id);
        return bankReconciliationSessionRepository.findById(id)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public BankReconciliationDto close(String id, CloseBankReconciliationRequest request, UserDto user) {
        log.info("BankReconciliationServiceImpl -> close: {}, user={}", id,
                user != null ? user.getNumberIdentity() : "unknown");

        String userId = user != null ? user.getNumberIdentity() : "unknown";
        String userName = user != null ? user.getFullName() : "unknown";

        BankReconciliationSession session = bankReconciliationSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conciliación bancaria no encontrada"));

        if (session.getStatus() != ECashCountStatus.EN_PROGRESO) {
            throw new RuntimeException("Solo se pueden cerrar conciliaciones en progreso");
        }

        session.setStatus(ECashCountStatus.CERRADO);
        ensureAuditTrail(session).add(AuditEntry.builder()
                .userId(userId)
                .userName(userName)
                .action(EAuditAction.CIERRE)
                .timestamp(LocalDateTime.now())
                .build());

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            String existingNotes = session.getNotes() != null ? session.getNotes() + " | " : "";
            session.setNotes(existingNotes + "Cierre: " + request.getNotes());
        }

        session = bankReconciliationSessionRepository.save(session);
        log.info("Bank reconciliation session closed: {}", id);

        return mapToDto(session);
    }

    @Override
    @Transactional
    public BankReconciliationDto cancel(String id, CancelBankReconciliationRequest request, UserDto user) {
        log.info("BankReconciliationServiceImpl -> cancel: {}, user={}", id,
                user != null ? user.getNumberIdentity() : "unknown");

        String userId = user != null ? user.getNumberIdentity() : "unknown";
        String userName = user != null ? user.getFullName() : "unknown";

        BankReconciliationSession session = bankReconciliationSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conciliación bancaria no encontrada"));

        if (session.getStatus() == ECashCountStatus.CERRADO) {
            throw new RuntimeException("No se puede anular una conciliación bancaria cerrada");
        }

        session.setStatus(ECashCountStatus.ANULADO);
        session.setCancelReason(request.getReason());
        ensureAuditTrail(session).add(AuditEntry.builder()
                .userId(userId)
                .userName(userName)
                .action(EAuditAction.ANULACION)
                .timestamp(LocalDateTime.now())
                .details(request.getReason())
                .build());

        session = bankReconciliationSessionRepository.save(session);
        log.info("Bank reconciliation session cancelled: {}", id);

        return mapToDto(session);
    }

    @Override
    @Transactional
    public BankReconciliationDto reopen(String id, String reason, UserDto user) {
        log.info("BankReconciliationServiceImpl -> reopen: {}, user={}", id,
                user != null ? user.getNumberIdentity() : "unknown");

        BankReconciliationSession session = bankReconciliationSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conciliación bancaria no encontrada"));

        if (session.getStatus() != ECashCountStatus.CERRADO) {
            throw new RuntimeException("Solo se pueden reabrir conciliaciones cerradas");
        }

        SessionSnapshot snapshot = SessionSnapshot.builder()
                .snapshotAt(LocalDateTime.now())
                .userId(user != null ? user.getNumberIdentity() : null)
                .userName(user != null ? user.getFullName() : null)
                .totalCounted(session.getTotalBankCounted())
                .expectedTotal(session.getExpectedBankTotal())
                .difference(session.getDifference())
                .reason(reason)
                .build();

        if (session.getSnapshots() == null) session.setSnapshots(new ArrayList<>());
        session.getSnapshots().add(snapshot);

        session.setStatus(ECashCountStatus.EN_PROGRESO);
        ensureAuditTrail(session).add(AuditEntry.builder()
                .userId(user != null ? user.getNumberIdentity() : null)
                .userName(user != null ? user.getFullName() : null)
                .action(EAuditAction.REAPERTURA)
                .timestamp(LocalDateTime.now())
                .details(reason)
                .build());

        session = bankReconciliationSessionRepository.save(session);
        log.info("Bank reconciliation session reopened: {}", id);
        return mapToDto(session);
    }

    @Override
    public List<BankReconciliationSummaryDto> list(LocalDate fromDate, LocalDate toDate, ECashCountStatus status, String bankAccountId) {
        log.info("BankReconciliationServiceImpl -> list: from={}, to={}, status={}, bankAccountId={}", fromDate, toDate, status, bankAccountId);

        List<BankReconciliationSession> sessions;

        if (bankAccountId != null && !bankAccountId.isBlank()) {
            if (fromDate != null && toDate != null && status != null) {
                sessions = bankReconciliationSessionRepository.findByBankAccountIdAndSessionDateBetweenAndStatus(bankAccountId, fromDate, toDate, status);
            } else if (fromDate != null && toDate != null) {
                sessions = bankReconciliationSessionRepository.findByBankAccountIdAndSessionDateBetween(bankAccountId, fromDate, toDate);
            } else if (status != null) {
                sessions = bankReconciliationSessionRepository.findByBankAccountIdAndStatus(bankAccountId, status);
            } else {
                sessions = bankReconciliationSessionRepository.findByBankAccountId(bankAccountId);
            }
        } else {
            sessions = bankReconciliationSessionRepository.findAll();
        }

        return sessions.stream()
                .map(this::mapToSummaryDto)
                .sorted(Comparator.comparing(BankReconciliationSummaryDto::getDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public SuggestedOpeningResponse getSuggestedOpening(String bankAccountId) {
        log.info("BankReconciliationServiceImpl -> getSuggestedOpening: bankAccountId={}", bankAccountId);

        List<BankReconciliationSession> closedSessions = bankReconciliationSessionRepository
                .findLastClosedSessionByBankAccountId(bankAccountId);

        if (closedSessions.isEmpty()) {
            return SuggestedOpeningResponse.builder()
                    .balance(BigDecimal.ZERO)
                    .lastCloseDate(null)
                    .build();
        }

        BankReconciliationSession lastClosed = closedSessions.get(0);
        return SuggestedOpeningResponse.builder()
                .balance(lastClosed.getTotalBankCounted())
                .lastCloseDate(lastClosed.getSessionDate())
                .build();
    }

    // ==================== Private methods ====================

    private BigDecimal resolveOpeningBalance(LocalDate date, String bankAccountId) {
        Optional<BankReconciliationSession> existing = bankReconciliationSessionRepository
                .findBySessionDateAndBankAccountId(date, bankAccountId);
        if (existing.isPresent() && existing.get().getOpeningBalance() != null) {
            return existing.get().getOpeningBalance();
        }

        List<BankReconciliationSession> closedSessions = bankReconciliationSessionRepository
                .findLastClosedSessionByBankAccountId(bankAccountId);
        if (!closedSessions.isEmpty() && closedSessions.get(0).getTotalBankCounted() != null) {
            return closedSessions.get(0).getTotalBankCounted();
        }

        return BigDecimal.ZERO;
    }

    private List<CashTransactionDto> getSalesTransactions(LocalDate date) {
        OffsetDateTime startOfDay = date.atStartOfDay().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(LocalTime.MAX).atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime();

        Query query = new Query(Criteria.where("dateTimeRecord").gte(startOfDay).lte(endOfDay)
                .and("saleType").is(EPaymentType.CONTADO));
        List<Billing> billings = mongoTemplate.find(query, Billing.class);

        List<CashTransactionDto> transactions = new ArrayList<>();

        for (Billing billing : billings) {
            if (billing.getPayments() != null && !billing.getPayments().isEmpty()) {
                for (PaymentEntry payment : billing.getPayments()) {
                    EPaymentMethod method = parsePaymentMethod(payment.getMethod());
                    if (method == EPaymentMethod.EFECTIVO) {
                        continue;
                    }
                    if (payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        transactions.add(CashTransactionDto.builder()
                                .id(billing.getId() + "-" + method.name())
                                .type(ETransactionType.INGRESO)
                                .category(ETransactionCategory.VENTA)
                                .description("Venta #" + billing.getBillNumber() + " - " + method.name())
                                .amount(payment.getAmount())
                                .paymentMethod(method)
                                .reference(payment.getReference())
                                .transactionDate(billing.getDateTimeRecord().toLocalDateTime())
                                .relatedDocumentId(billing.getId())
                                .build());
                    }
                }
            } else {
                EPaymentMethod paymentMethod = billing.getPaymentMethods() != null &&
                        !billing.getPaymentMethods().isEmpty()
                        ? billing.getPaymentMethods().get(0)
                        : EPaymentMethod.EFECTIVO;

                if (paymentMethod != EPaymentMethod.EFECTIVO) {
                    transactions.add(CashTransactionDto.builder()
                            .id(billing.getId())
                            .type(ETransactionType.INGRESO)
                            .category(ETransactionCategory.VENTA)
                            .description("Venta #" + billing.getBillNumber())
                            .amount(billing.getTotalBilling())
                            .paymentMethod(paymentMethod)
                            .transactionDate(billing.getDateTimeRecord().toLocalDateTime())
                            .relatedDocumentId(billing.getId())
                            .build());
                }
            }
        }

        return transactions;
    }

    private EPaymentMethod parsePaymentMethod(String method) {
        if (method == null) return EPaymentMethod.EFECTIVO;
        try {
            return EPaymentMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            return EPaymentMethod.EFECTIVO;
        }
    }

    private List<CashTransactionDto> getCreditPaymentTransactions(LocalDate date) {
        List<CashTransactionDto> transactions = new ArrayList<>();

        List<ClientAccount> allAccounts = clientAccountRepository.findAll();
        for (ClientAccount account : allAccounts) {
            if (account.getPayments() != null) {
                for (AccountPayment payment : account.getPayments()) {
                    EPaymentMethod method = payment.getPaymentMethod() != null ?
                            payment.getPaymentMethod() : EPaymentMethod.EFECTIVO;
                    if (method == EPaymentMethod.EFECTIVO) continue;

                    if (payment.getPaymentDate() != null &&
                            payment.getPaymentDate().toLocalDate().equals(date)) {

                        String clientName = account.getClient() != null ?
                                account.getClient().getFullName() : "Cliente";

                        transactions.add(CashTransactionDto.builder()
                                .id(payment.getId())
                                .type(ETransactionType.INGRESO)
                                .category(ETransactionCategory.PAGO_CREDITO)
                                .description("Abono crédito - " + clientName)
                                .amount(payment.getAmount())
                                .paymentMethod(method)
                                .reference(payment.getReference())
                                .transactionDate(payment.getPaymentDate())
                                .relatedDocumentId(account.getId())
                                .build());
                    }
                }
            }
        }

        return transactions;
    }

    private List<CashTransactionDto> getDepositTransactions(LocalDate date) {
        List<CashTransactionDto> transactions = new ArrayList<>();

        List<ClientCredit> allCredits = clientCreditRepository.findAll();
        for (ClientCredit credit : allCredits) {
            if (credit.getTransactions() != null) {
                for (CreditTransaction ct : credit.getTransactions()) {
                    EPaymentMethod method = ct.getPaymentMethod() != null ?
                            ct.getPaymentMethod() : EPaymentMethod.EFECTIVO;
                    if (method == EPaymentMethod.EFECTIVO) continue;

                    if (ct.getType() == ECreditTransactionType.DEPOSIT &&
                            ct.getTransactionDate() != null &&
                            ct.getTransactionDate().toLocalDate().equals(date)) {

                        String clientName = credit.getClient() != null ?
                                credit.getClient().getFullName() : "Cliente";

                        transactions.add(CashTransactionDto.builder()
                                .id(ct.getId())
                                .type(ETransactionType.INGRESO)
                                .category(ETransactionCategory.DEPOSITO_ANTICIPO)
                                .description("Depósito saldo a favor - " + clientName)
                                .amount(ct.getAmount())
                                .paymentMethod(method)
                                .reference(ct.getReference())
                                .transactionDate(ct.getTransactionDate())
                                .relatedDocumentId(credit.getId())
                                .build());
                    }
                }
            }
        }

        return transactions;
    }

    private List<CashTransactionDto> getRefundTransactions(LocalDate date) {
        List<CashTransactionDto> transactions = new ArrayList<>();

        List<ClientCredit> allCredits = clientCreditRepository.findAll();
        for (ClientCredit credit : allCredits) {
            if (credit.getTransactions() != null) {
                for (CreditTransaction ct : credit.getTransactions()) {
                    EPaymentMethod method = ct.getPaymentMethod() != null ?
                            ct.getPaymentMethod() : EPaymentMethod.EFECTIVO;
                    if (method == EPaymentMethod.EFECTIVO) continue;

                    if (ct.getType() == ECreditTransactionType.REFUND &&
                            ct.getTransactionDate() != null &&
                            ct.getTransactionDate().toLocalDate().equals(date)) {

                        String clientName = credit.getClient() != null ?
                                credit.getClient().getFullName() : "Cliente";

                        transactions.add(CashTransactionDto.builder()
                                .id(ct.getId())
                                .type(ETransactionType.EGRESO)
                                .category(ETransactionCategory.DEVOLUCION_ANTICIPO)
                                .description("Devolución saldo a favor - " + clientName)
                                .amount(ct.getAmount())
                                .paymentMethod(method)
                                .reference(ct.getReference())
                                .transactionDate(ct.getTransactionDate())
                                .relatedDocumentId(credit.getId())
                                .build());
                    }
                }
            }
        }

        return transactions;
    }

    private List<CashTransactionDto> getExpenseTransactions(LocalDate date) {
        OffsetDateTime startOfDay = date.atStartOfDay().atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(LocalTime.MAX).atZone(DateTimeUtil.getBogotaZone()).toOffsetDateTime();

        Query query = new Query(Criteria.where("dateTimeRecord").gte(startOfDay).lte(endOfDay));
        List<Expense> expenses = mongoTemplate.find(query, Expense.class);

        return expenses.stream()
                .filter(expense -> {
                    EPaymentMethod method = expense.getPaymentMethod() != null ?
                            expense.getPaymentMethod() : EPaymentMethod.EFECTIVO;
                    return method != EPaymentMethod.EFECTIVO;
                })
                .map(expense -> CashTransactionDto.builder()
                        .id(expense.getId())
                        .type(ETransactionType.EGRESO)
                        .category(ETransactionCategory.GASTO)
                        .description(expense.getDescription())
                        .amount(expense.getAmount())
                        .paymentMethod(expense.getPaymentMethod())
                        .reference(expense.getReference())
                        .transactionDate(expense.getDateTimeRecord().toLocalDateTime())
                        .relatedDocumentId(expense.getId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CashTransactionDto> getSupplierPaymentTransactions(LocalDate date) {
        List<SupplierPayment> payments = supplierPaymentRepository.findByPaymentDate(date);

        return payments.stream()
                .filter(payment -> {
                    EPaymentMethod method = payment.getMethod() != null ?
                            payment.getMethod() : EPaymentMethod.EFECTIVO;
                    return method != EPaymentMethod.EFECTIVO;
                })
                .map(payment -> CashTransactionDto.builder()
                        .id(payment.getId())
                        .type(ETransactionType.EGRESO)
                        .category(ETransactionCategory.PAGO_PROVEEDOR)
                        .description("Pago a " + payment.getSupplierName())
                        .amount(payment.getAmount())
                        .paymentMethod(payment.getMethod())
                        .reference(payment.getReference())
                        .transactionDate(date.atStartOfDay())
                        .relatedDocumentId(payment.getId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CashTransactionDto> getInternalTransferTransactions(LocalDate date, String bankAccountId) {
        List<InternalTransfer> transfers = (bankAccountId != null && !bankAccountId.isBlank())
                ? internalTransferRepository.findByTransferDateAndStatusAndDestinationBankAccountId(
                        date, EInternalTransferStatus.ACTIVO, bankAccountId)
                : internalTransferRepository.findByTransferDateAndStatus(date, EInternalTransferStatus.ACTIVO);

        return transfers.stream()
                .map(t -> CashTransactionDto.builder()
                        .id(t.getId())
                        .type(ETransactionType.INGRESO)
                        .category(ETransactionCategory.TRASLADO_BANCO)
                        .description(buildTransferDescription(t))
                        .amount(t.getAmount())
                        .paymentMethod(EPaymentMethod.TRANSFERENCIA)
                        .reference(t.getReference())
                        .transactionDate(t.getTransferDateTime() != null
                                ? t.getTransferDateTime() : date.atStartOfDay())
                        .relatedDocumentId(t.getId())
                        .build())
                .collect(Collectors.toList());
    }

    private String buildTransferDescription(InternalTransfer t) {
        StringBuilder sb = new StringBuilder("Traslado a banco");
        if (t.getDestinationBankName() != null && !t.getDestinationBankName().isBlank()) {
            sb.append(" - ").append(t.getDestinationBankName());
        }
        if (t.getDestinationAccountNumber() != null && !t.getDestinationAccountNumber().isBlank()) {
            sb.append(" (").append(t.getDestinationAccountNumber()).append(")");
        }
        return sb.toString();
    }

    private List<PaymentMethodSummaryDto> calculatePaymentMethodSummaries(List<CashTransactionDto> transactions) {
        Map<EPaymentMethod, PaymentMethodSummaryDto> summaryMap = new HashMap<>();

        for (CashTransactionDto t : transactions) {
            EPaymentMethod method = t.getPaymentMethod() != null ? t.getPaymentMethod() : EPaymentMethod.EFECTIVO;

            PaymentMethodSummaryDto summary = summaryMap.computeIfAbsent(method, m ->
                    PaymentMethodSummaryDto.builder()
                            .paymentMethod(m)
                            .totalIncome(BigDecimal.ZERO)
                            .totalExpense(BigDecimal.ZERO)
                            .netAmount(BigDecimal.ZERO)
                            .transactionCount(0)
                            .build());

            if (t.getType() == ETransactionType.INGRESO) {
                summary.setTotalIncome(summary.getTotalIncome().add(t.getAmount()));
            } else {
                summary.setTotalExpense(summary.getTotalExpense().add(t.getAmount()));
            }
            summary.setTransactionCount(summary.getTransactionCount() + 1);
            summary.setNetAmount(summary.getTotalIncome().subtract(summary.getTotalExpense()));
        }

        return new ArrayList<>(summaryMap.values());
    }

    private BankReconciliationDto mapToDto(BankReconciliationSession session) {
        List<AuditEntryDto> auditDtos = session.getAuditTrail() != null ?
                session.getAuditTrail().stream()
                        .map(a -> AuditEntryDto.builder()
                                .userId(a.getUserId())
                                .userName(a.getUserName())
                                .action(a.getAction())
                                .timestamp(a.getTimestamp())
                                .details(a.getDetails())
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>();

        List<SessionSnapshotDto> snapshotDtos = session.getSnapshots() != null ?
                session.getSnapshots().stream()
                        .map(s -> SessionSnapshotDto.builder()
                                .snapshotAt(s.getSnapshotAt())
                                .userId(s.getUserId())
                                .userName(s.getUserName())
                                .totalCounted(s.getTotalCounted())
                                .expectedTotal(s.getExpectedTotal())
                                .difference(s.getDifference())
                                .reason(s.getReason())
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>();

        return BankReconciliationDto.builder()
                .id(session.getId())
                .sessionDate(session.getSessionDate())
                .bankAccountId(session.getBankAccountId())
                .bankAccountName(session.getBankAccountName())
                .openingBalance(session.getOpeningBalance())
                .totalBankCounted(session.getTotalBankCounted())
                .expectedBankAmount(session.getExpectedBankAmount())
                .expectedBankTotal(session.getExpectedBankTotal())
                .difference(session.getDifference())
                .totalIncome(session.getTotalIncome())
                .totalExpense(session.getTotalExpense())
                .totalTransfers(session.getTotalTransfers())
                .netBankFlow(session.getNetBankFlow())
                .status(session.getStatus())
                .notes(session.getNotes())
                .cancelReason(session.getCancelReason())
                .auditTrail(auditDtos)
                .snapshots(snapshotDtos)
                .build();
    }

    private List<AuditEntry> ensureAuditTrail(BankReconciliationSession session) {
        if (session.getAuditTrail() == null) {
            session.setAuditTrail(new ArrayList<>());
        }
        return session.getAuditTrail();
    }

    private BankReconciliationSummaryDto mapToSummaryDto(BankReconciliationSession session) {
        List<AuditEntryDto> auditDtos = session.getAuditTrail() != null ?
                session.getAuditTrail().stream()
                        .map(a -> AuditEntryDto.builder()
                                .userId(a.getUserId())
                                .userName(a.getUserName())
                                .action(a.getAction())
                                .timestamp(a.getTimestamp())
                                .details(a.getDetails())
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>();

        return BankReconciliationSummaryDto.builder()
                .date(session.getSessionDate())
                .bankAccountId(session.getBankAccountId())
                .bankAccountName(session.getBankAccountName())
                .openingBalance(session.getOpeningBalance())
                .totalIncome(session.getTotalIncome())
                .totalExpense(session.getTotalExpense())
                .expectedBank(session.getExpectedBankAmount())
                .countedBank(session.getTotalBankCounted())
                .difference(session.getDifference())
                .status(session.getStatus())
                .auditTrail(auditDtos)
                .build();
    }
}
