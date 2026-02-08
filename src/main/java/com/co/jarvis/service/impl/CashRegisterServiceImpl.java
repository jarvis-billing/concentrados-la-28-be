package com.co.jarvis.service.impl;

import com.co.jarvis.dto.cashregister.*;
import com.co.jarvis.entity.*;
import com.co.jarvis.enums.*;
import com.co.jarvis.repository.*;
import com.co.jarvis.service.CashRegisterService;
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
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashRegisterServiceImpl implements CashRegisterService {

    private final CashCountSessionRepository cashCountSessionRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final ClientCreditRepository clientCreditRepository;
    private final ClientAccountRepository clientAccountRepository;
    private final MongoTemplate mongoTemplate;

    // Denominaciones colombianas
    private static final Map<Integer, EDenominationType> COLOMBIAN_DENOMINATIONS = new LinkedHashMap<>();
    static {
        // Billetes
        COLOMBIAN_DENOMINATIONS.put(100000, EDenominationType.BILLETE);
        COLOMBIAN_DENOMINATIONS.put(50000, EDenominationType.BILLETE);
        COLOMBIAN_DENOMINATIONS.put(20000, EDenominationType.BILLETE);
        COLOMBIAN_DENOMINATIONS.put(10000, EDenominationType.BILLETE);
        COLOMBIAN_DENOMINATIONS.put(5000, EDenominationType.BILLETE);
        COLOMBIAN_DENOMINATIONS.put(2000, EDenominationType.BILLETE);
        COLOMBIAN_DENOMINATIONS.put(1000, EDenominationType.BILLETE);
        // Monedas
        COLOMBIAN_DENOMINATIONS.put(500, EDenominationType.MONEDA);
        COLOMBIAN_DENOMINATIONS.put(200, EDenominationType.MONEDA);
        COLOMBIAN_DENOMINATIONS.put(100, EDenominationType.MONEDA);
        COLOMBIAN_DENOMINATIONS.put(50, EDenominationType.MONEDA);
    }

    @Override
    public DailySummaryResponse getDailySummary(LocalDate date) {
        log.info("CashRegisterServiceImpl -> getDailySummary: {}", date);

        List<CashTransactionDto> transactions = new ArrayList<>();

        // 1. Obtener ventas del día
        transactions.addAll(getSalesTransactions(date));

        // 2. Obtener pagos de crédito (abonos a cuentas por cobrar)
        transactions.addAll(getCreditPaymentTransactions(date));

        // 3. Obtener depósitos/anticipos (saldos a favor)
        transactions.addAll(getDepositTransactions(date));

        // 4. Obtener gastos
        transactions.addAll(getExpenseTransactions(date));

        // 5. Obtener pagos a proveedores
        transactions.addAll(getSupplierPaymentTransactions(date));

        // Calcular resúmenes por método de pago
        List<PaymentMethodSummaryDto> paymentMethodSummaries = calculatePaymentMethodSummaries(transactions);

        // Calcular totales
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == ETransactionType.INGRESO)
                .map(CashTransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() == ETransactionType.EGRESO)
                .map(CashTransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular montos esperados por método de pago
        BigDecimal expectedCash = paymentMethodSummaries.stream()
                .filter(s -> s.getPaymentMethod() == EPaymentMethod.EFECTIVO)
                .map(PaymentMethodSummaryDto::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedTransfer = paymentMethodSummaries.stream()
                .filter(s -> s.getPaymentMethod() == EPaymentMethod.TRANSFERENCIA)
                .map(PaymentMethodSummaryDto::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedOther = paymentMethodSummaries.stream()
                .filter(s -> s.getPaymentMethod() != EPaymentMethod.EFECTIVO && 
                            s.getPaymentMethod() != EPaymentMethod.TRANSFERENCIA)
                .map(PaymentMethodSummaryDto::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailySummaryResponse.builder()
                .transactions(transactions)
                .paymentMethodSummaries(paymentMethodSummaries)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .expectedCashAmount(expectedCash)
                .expectedTransferAmount(expectedTransfer)
                .expectedOtherAmount(expectedOther)
                .build();
    }

    @Override
    @Transactional
    public CashCountSessionDto createOrUpdate(CreateCashCountRequest request, String createdBy) {
        log.info("CashRegisterServiceImpl -> createOrUpdate: date={}", request.getSessionDate());

        // Buscar si ya existe un arqueo para esa fecha
        Optional<CashCountSession> existingSession = cashCountSessionRepository
                .findBySessionDate(request.getSessionDate());

        CashCountSession session;

        if (existingSession.isPresent()) {
            session = existingSession.get();
            
            // Si está cerrado, retornar error
            if (session.getStatus() == ECashCountStatus.CERRADO) {
                throw new RuntimeException("El arqueo de esta fecha ya está cerrado y no puede modificarse");
            }
            
            // Si está anulado, retornar error
            if (session.getStatus() == ECashCountStatus.ANULADO) {
                throw new RuntimeException("El arqueo de esta fecha está anulado. Debe crear uno nuevo.");
            }
            
            // Actualizar el existente
            session.setOpeningBalance(request.getOpeningBalance());
            session.setNotes(request.getNotes());
            session.setUpdatedAt(LocalDateTime.now());
        } else {
            // Crear nuevo
            session = CashCountSession.builder()
                    .sessionDate(request.getSessionDate())
                    .openingBalance(request.getOpeningBalance())
                    .notes(request.getNotes())
                    .status(ECashCountStatus.EN_PROGRESO)
                    .createdBy(createdBy)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        // Procesar denominaciones
        List<CashDenomination> denominations = processDenominations(request.getCashDenominations());
        session.setCashDenominations(denominations);

        // Calcular total contado
        BigDecimal totalCounted = denominations.stream()
                .map(CashDenomination::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        session.setTotalCashCounted(totalCounted);

        // Obtener datos del resumen diario
        DailySummaryResponse dailySummary = getDailySummary(request.getSessionDate());
        
        session.setTotalIncome(dailySummary.getTotalIncome());
        session.setTotalExpense(dailySummary.getTotalExpense());
        session.setExpectedCashAmount(dailySummary.getExpectedCashAmount());
        session.setExpectedTransferAmount(dailySummary.getExpectedTransferAmount());
        session.setExpectedOtherAmount(dailySummary.getExpectedOtherAmount());

        // Calcular diferencia de efectivo
        // Efectivo esperado = saldo apertura + ingresos efectivo - egresos efectivo
        BigDecimal expectedCashTotal = request.getOpeningBalance()
                .add(dailySummary.getExpectedCashAmount());
        session.setCashDifference(totalCounted.subtract(expectedCashTotal));

        // Calcular flujo neto
        session.setNetCashFlow(dailySummary.getTotalIncome().subtract(dailySummary.getTotalExpense()));

        // Guardar
        session = cashCountSessionRepository.save(session);
        log.info("Cash count session saved with ID: {}", session.getId());

        return mapToDto(session);
    }

    @Override
    public CashCountSessionDto getByDate(LocalDate date) {
        log.info("CashRegisterServiceImpl -> getByDate: {}", date);
        return cashCountSessionRepository.findBySessionDate(date)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    public CashCountSessionDto getById(String id) {
        log.info("CashRegisterServiceImpl -> getById: {}", id);
        return cashCountSessionRepository.findById(id)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public CashCountSessionDto close(String id, CloseCashCountRequest request, String closedBy) {
        log.info("CashRegisterServiceImpl -> close: {}", id);

        CashCountSession session = cashCountSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arqueo no encontrado"));

        if (session.getStatus() != ECashCountStatus.EN_PROGRESO) {
            throw new RuntimeException("Solo se pueden cerrar arqueos en progreso");
        }

        session.setStatus(ECashCountStatus.CERRADO);
        session.setClosedBy(closedBy);
        session.setClosedAt(LocalDateTime.now());
        
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            String existingNotes = session.getNotes() != null ? session.getNotes() + " | " : "";
            session.setNotes(existingNotes + "Cierre: " + request.getNotes());
        }

        session = cashCountSessionRepository.save(session);
        log.info("Cash count session closed: {}", id);

        return mapToDto(session);
    }

    @Override
    @Transactional
    public CashCountSessionDto cancel(String id, CancelCashCountRequest request, String cancelledBy) {
        log.info("CashRegisterServiceImpl -> cancel: {}", id);

        CashCountSession session = cashCountSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arqueo no encontrado"));

        if (session.getStatus() == ECashCountStatus.CERRADO) {
            throw new RuntimeException("No se puede anular un arqueo cerrado");
        }

        session.setStatus(ECashCountStatus.ANULADO);
        session.setCancelReason(request.getReason());
        session.setClosedBy(cancelledBy);
        session.setClosedAt(LocalDateTime.now());

        session = cashCountSessionRepository.save(session);
        log.info("Cash count session cancelled: {}", id);

        return mapToDto(session);
    }

    @Override
    public List<CashCountSummaryDto> list(LocalDate fromDate, LocalDate toDate, ECashCountStatus status) {
        log.info("CashRegisterServiceImpl -> list: from={}, to={}, status={}", fromDate, toDate, status);

        List<CashCountSession> sessions;

        if (fromDate != null && toDate != null && status != null) {
            sessions = cashCountSessionRepository.findBySessionDateBetweenAndStatus(fromDate, toDate, status);
        } else if (fromDate != null && toDate != null) {
            sessions = cashCountSessionRepository.findBySessionDateBetween(fromDate, toDate);
        } else if (status != null) {
            sessions = cashCountSessionRepository.findByStatus(status);
        } else {
            sessions = cashCountSessionRepository.findAll();
        }

        return sessions.stream()
                .map(this::mapToSummaryDto)
                .sorted(Comparator.comparing(CashCountSummaryDto::getDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public SuggestedOpeningResponse getSuggestedOpening() {
        log.info("CashRegisterServiceImpl -> getSuggestedOpening");

        List<CashCountSession> closedSessions = cashCountSessionRepository.findLastClosedSession();
        
        if (closedSessions.isEmpty()) {
            return SuggestedOpeningResponse.builder()
                    .balance(BigDecimal.ZERO)
                    .lastCloseDate(null)
                    .build();
        }

        CashCountSession lastClosed = closedSessions.get(0);
        return SuggestedOpeningResponse.builder()
                .balance(lastClosed.getTotalCashCounted())
                .lastCloseDate(lastClosed.getSessionDate())
                .build();
    }

    // ==================== Métodos privados ====================

    private List<CashTransactionDto> getSalesTransactions(LocalDate date) {
        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = date.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        // Solo ventas de CONTADO - las ventas a crédito se registran cuando el cliente paga
        Query query = new Query(Criteria.where("dateTimeRecord").gte(startOfDay).lte(endOfDay)
                .and("saleType").is(EPaymentType.CONTADO));
        List<Billing> billings = mongoTemplate.find(query, Billing.class);

        List<CashTransactionDto> transactions = new ArrayList<>();
        
        for (Billing billing : billings) {
            // Si tiene detalle de payments, usar esos para desglosar por método de pago
            if (billing.getPayments() != null && !billing.getPayments().isEmpty()) {
                for (PaymentEntry payment : billing.getPayments()) {
                    EPaymentMethod method = parsePaymentMethod(payment.getMethod());
                    transactions.add(CashTransactionDto.builder()
                            .id(billing.getId() + "-" + payment.getMethod())
                            .type(ETransactionType.INGRESO)
                            .category(ETransactionCategory.VENTA)
                            .description("Venta #" + billing.getBillNumber())
                            .amount(payment.getAmount())
                            .paymentMethod(method)
                            .reference(payment.getReference())
                            .transactionDate(billing.getDateTimeRecord().toLocalDateTime())
                            .relatedDocumentId(billing.getId())
                            .build());
                }
            } else {
                // Fallback: usar paymentMethods array (monto total)
                EPaymentMethod paymentMethod = billing.getPaymentMethods() != null && 
                        !billing.getPaymentMethods().isEmpty() 
                        ? billing.getPaymentMethods().get(0) 
                        : EPaymentMethod.EFECTIVO;

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
        // Buscar pagos de crédito (abonos a cuentas por cobrar) desde ClientAccount.payments[]
        List<CashTransactionDto> transactions = new ArrayList<>();
        
        List<ClientAccount> allAccounts = clientAccountRepository.findAll();
        for (ClientAccount account : allAccounts) {
            if (account.getPayments() != null) {
                for (AccountPayment payment : account.getPayments()) {
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
                                .paymentMethod(payment.getPaymentMethod() != null ? 
                                        payment.getPaymentMethod() : EPaymentMethod.EFECTIVO)
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
        // Depósitos/anticipos (saldos a favor) desde ClientCredit.transactions[]
        List<CashTransactionDto> transactions = new ArrayList<>();
        
        List<ClientCredit> allCredits = clientCreditRepository.findAll();
        for (ClientCredit credit : allCredits) {
            if (credit.getTransactions() != null) {
                for (CreditTransaction ct : credit.getTransactions()) {
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
                                .paymentMethod(ct.getPaymentMethod() != null ? 
                                        ct.getPaymentMethod() : EPaymentMethod.EFECTIVO)
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
        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = date.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        Query query = new Query(Criteria.where("dateTimeRecord").gte(startOfDay).lte(endOfDay));
        List<Expense> expenses = mongoTemplate.find(query, Expense.class);

        return expenses.stream()
                .map(expense -> CashTransactionDto.builder()
                        .id(expense.getId())
                        .type(ETransactionType.EGRESO)
                        .category(ETransactionCategory.GASTO)
                        .description(expense.getDescription())
                        .amount(expense.getAmount())
                        .paymentMethod(expense.getPaymentMethod() != null ? 
                                expense.getPaymentMethod() : EPaymentMethod.EFECTIVO)
                        .reference(expense.getReference())
                        .transactionDate(expense.getDateTimeRecord().toLocalDateTime())
                        .relatedDocumentId(expense.getId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CashTransactionDto> getSupplierPaymentTransactions(LocalDate date) {
        List<SupplierPayment> payments = supplierPaymentRepository.findByPaymentDate(date);

        return payments.stream()
                .map(payment -> CashTransactionDto.builder()
                        .id(payment.getId())
                        .type(ETransactionType.EGRESO)
                        .category(ETransactionCategory.PAGO_PROVEEDOR)
                        .description("Pago a " + payment.getSupplierName())
                        .amount(payment.getAmount())
                        .paymentMethod(payment.getMethod() != null ? 
                                payment.getMethod() : EPaymentMethod.EFECTIVO)
                        .reference(payment.getReference())
                        .transactionDate(date.atStartOfDay())
                        .relatedDocumentId(payment.getId())
                        .build())
                .collect(Collectors.toList());
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

    private List<CashDenomination> processDenominations(List<CashDenominationDto> dtos) {
        if (dtos == null) return new ArrayList<>();

        return dtos.stream()
                .filter(dto -> dto.getQuantity() != null && dto.getQuantity() > 0)
                .map(dto -> {
                    EDenominationType type = COLOMBIAN_DENOMINATIONS.getOrDefault(
                            dto.getValue(), EDenominationType.BILLETE);
                    BigDecimal subtotal = BigDecimal.valueOf(dto.getValue())
                            .multiply(BigDecimal.valueOf(dto.getQuantity()));

                    return CashDenomination.builder()
                            .value(dto.getValue())
                            .type(type)
                            .quantity(dto.getQuantity())
                            .subtotal(subtotal)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private CashCountSessionDto mapToDto(CashCountSession session) {
        List<CashDenominationDto> denominationDtos = session.getCashDenominations() != null ?
                session.getCashDenominations().stream()
                        .map(d -> CashDenominationDto.builder()
                                .value(d.getValue())
                                .quantity(d.getQuantity())
                                .type(d.getType() != null ? d.getType().name() : null)
                                .subtotal(d.getSubtotal())
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>();

        return CashCountSessionDto.builder()
                .id(session.getId())
                .sessionDate(session.getSessionDate())
                .openingBalance(session.getOpeningBalance())
                .cashDenominations(denominationDtos)
                .totalCashCounted(session.getTotalCashCounted())
                .expectedCashAmount(session.getExpectedCashAmount())
                .expectedTransferAmount(session.getExpectedTransferAmount())
                .expectedOtherAmount(session.getExpectedOtherAmount())
                .cashDifference(session.getCashDifference())
                .totalIncome(session.getTotalIncome())
                .totalExpense(session.getTotalExpense())
                .netCashFlow(session.getNetCashFlow())
                .status(session.getStatus())
                .notes(session.getNotes())
                .cancelReason(session.getCancelReason())
                .createdBy(session.getCreatedBy())
                .createdAt(session.getCreatedAt())
                .closedBy(session.getClosedBy())
                .closedAt(session.getClosedAt())
                .build();
    }

    private CashCountSummaryDto mapToSummaryDto(CashCountSession session) {
        return CashCountSummaryDto.builder()
                .date(session.getSessionDate())
                .openingBalance(session.getOpeningBalance())
                .totalIncome(session.getTotalIncome())
                .totalExpense(session.getTotalExpense())
                .expectedCash(session.getExpectedCashAmount())
                .countedCash(session.getTotalCashCounted())
                .difference(session.getCashDifference())
                .status(session.getStatus())
                .closedBy(session.getClosedBy())
                .build();
    }
}
