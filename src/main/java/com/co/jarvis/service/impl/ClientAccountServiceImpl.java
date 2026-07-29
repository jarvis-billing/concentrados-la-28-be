package com.co.jarvis.service.impl;

import com.co.jarvis.dto.AccountReportFilter;
import com.co.jarvis.dto.AccountSummary;
import com.co.jarvis.dto.BillingDto;
import com.co.jarvis.dto.ManualDebtRequest;
import com.co.jarvis.dto.PagedAccountReport;
import com.co.jarvis.dto.RegisterPaymentRequest;
import com.co.jarvis.entity.AccountPayment;
import com.co.jarvis.entity.AccountTransaction;
import com.co.jarvis.entity.Billing;
import com.co.jarvis.entity.Client;
import com.co.jarvis.entity.ClientAccount;
import com.co.jarvis.enums.EAccountTransactionType;
import com.co.jarvis.enums.EPaymentType;
import com.co.jarvis.repository.ClientAccountRepository;
import com.co.jarvis.repository.ClientRepository;
import com.co.jarvis.service.ClientAccountService;
import com.co.jarvis.util.BankAccountHelper;
import com.co.jarvis.util.mappers.GenericMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAccountServiceImpl implements ClientAccountService {

    private final ClientAccountRepository clientAccountRepository;
    private final ClientRepository clientRepository;
    private final MongoTemplate mongoTemplate;
    private final BankAccountHelper bankAccountHelper;

    private final GenericMapper<Billing, BillingDto> billingMapper = 
            new GenericMapper<>(Billing.class, BillingDto.class);

    @Override
    public ClientAccount getByClientId(String clientId) {
        log.info("ClientAccountServiceImpl -> getByClientId: {}", clientId);
        return clientAccountRepository.findByClientId(clientId).orElse(null);
    }

    @Override
    public BigDecimal getClientBalance(String clientId) {
        log.info("ClientAccountServiceImpl -> getClientBalance: {}", clientId);
        return clientAccountRepository.findByClientId(clientId)
                .map(ClientAccount::getCurrentBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public List<ClientAccount> getAllWithBalance() {
        log.info("ClientAccountServiceImpl -> getAllWithBalance");
        return clientAccountRepository.findAllWithBalance(BigDecimal.ZERO);
    }

    @Override
    public List<AccountPayment> getPaymentsByClientId(String clientId) {
        log.info("ClientAccountServiceImpl -> getPaymentsByClientId: {}", clientId);
        return clientAccountRepository.findByClientId(clientId)
                .map(ClientAccount::getPayments)
                .orElse(Collections.emptyList());
    }

    @Override
    public List<BillingDto> getCreditBillingsByClientId(String clientId) {
        log.info("ClientAccountServiceImpl -> getCreditBillingsByClientId: {}", clientId);
        Query query = new Query();
        query.addCriteria(Criteria.where("client.id").is(clientId));
        query.addCriteria(Criteria.where("saleType").is(EPaymentType.CREDITO));
        
        List<Billing> billings = mongoTemplate.find(query, Billing.class);
        return billingMapper.mapToDtoList(billings);
    }

    @Override
    @Transactional
    public void addDebt(String clientId, BigDecimal amount) {
        log.info("ClientAccountServiceImpl -> addDebt: clientId={}, amount={}", clientId, amount);
        
        ClientAccount account = clientAccountRepository.findByClientId(clientId)
                .orElseGet(() -> createNewAccount(clientId));

        account.setTotalDebt(account.getTotalDebt().add(amount));
        account.setCurrentBalance(account.getTotalDebt().subtract(account.getTotalPaid()));
        account.setUpdatedAt(LocalDateTime.now());

        clientAccountRepository.save(account);
        log.info("Debt added successfully. New balance: {}", account.getCurrentBalance());
    }

    @Override
    @Transactional
    public AccountPayment registerPayment(RegisterPaymentRequest request, String createdBy) {
        log.info("ClientAccountServiceImpl -> registerPayment: clientId={}, amount={}", 
                request.getClientAccountId(), request.getAmount());

        ClientAccount account = clientAccountRepository.findByClientId(request.getClientAccountId())
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada para el cliente"));

        if (request.getAmount().compareTo(account.getCurrentBalance()) > 0) {
            throw new RuntimeException("El monto del pago excede el saldo pendiente");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto del pago debe ser mayor a cero");
        }

        // Validar y enriquecer info bancaria si es transferencia
        bankAccountHelper.requireBankAccountForTransfer(request.getPaymentMethod(), request.getBankAccountId());
        String bankAccountName = bankAccountHelper.resolveBankAccountName(
                request.getBankAccountId(), request.getBankAccountName());

        AccountPayment payment = AccountPayment.builder()
                .id(UUID.randomUUID().toString())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .bankAccountId(request.getBankAccountId())
                .bankAccountName(bankAccountName)
                .reference(request.getReference())
                .notes(request.getNotes())
                .paymentDate(LocalDateTime.now())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        account.getPayments().add(payment);
        account.setTotalPaid(account.getTotalPaid().add(request.getAmount()));
        account.setCurrentBalance(account.getTotalDebt().subtract(account.getTotalPaid()));
        account.setLastPaymentDate(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        clientAccountRepository.save(account);
        log.info("Payment registered successfully. New balance: {}", account.getCurrentBalance());
        
        return payment;
    }

    @Override
    public List<AccountSummary> generateReport(AccountReportFilter filter) {
        log.info("ClientAccountServiceImpl -> generateReport filter={}", filter);

        boolean hasDateFilter   = filter.getFromDate() != null || filter.getToDate() != null;
        boolean onlyWithBalance = Boolean.TRUE.equals(filter.getOnlyWithBalance());

        LocalDateTime from = filter.getFromDate() != null
                ? filter.getFromDate().atStartOfDay() : null;
        LocalDateTime to   = filter.getToDate() != null
                ? filter.getToDate().atTime(LocalTime.MAX) : null;

        List<ClientAccount> accounts = fetchAccounts(filter, hasDateFilter, onlyWithBalance, from, to);

        int page = filter.getPage() != null ? filter.getPage() : 0;
        int size = filter.getSize() != null ? filter.getSize() : 50;
        List<ClientAccount> pageSlice = paginate(accounts, page, size);

        return pageSlice.stream()
                .map(acc -> mapToAccountSummary(acc, hasDateFilter ? from : null,
                                                    hasDateFilter ? to   : null))
                .collect(Collectors.toList());
    }

    @Override
    public PagedAccountReport generatePagedReport(AccountReportFilter filter) {
        log.info("ClientAccountServiceImpl -> generatePagedReport filter={}", filter);

        boolean hasDateFilter   = filter.getFromDate() != null || filter.getToDate() != null;
        boolean onlyWithBalance = Boolean.TRUE.equals(filter.getOnlyWithBalance());

        LocalDateTime from = filter.getFromDate() != null
                ? filter.getFromDate().atStartOfDay() : null;
        LocalDateTime to   = filter.getToDate() != null
                ? filter.getToDate().atTime(LocalTime.MAX) : null;

        List<ClientAccount> all = fetchAccounts(filter, hasDateFilter, onlyWithBalance, from, to);

        // Ordenar A-Z por apellido, luego por nombre
        all.sort((a, b) -> {
            String sA = a.getClient() != null && a.getClient().getSurname() != null
                    ? a.getClient().getSurname() : "";
            String sB = b.getClient() != null && b.getClient().getSurname() != null
                    ? b.getClient().getSurname() : "";
            int cmp = sA.compareToIgnoreCase(sB);
            if (cmp != 0) return cmp;
            String nA = a.getClient() != null && a.getClient().getName() != null
                    ? a.getClient().getName() : "";
            String nB = b.getClient() != null && b.getClient().getName() != null
                    ? b.getClient().getName() : "";
            return nA.compareToIgnoreCase(nB);
        });

        int page = filter.getPage() != null ? filter.getPage() : 0;
        int size = filter.getSize() != null ? filter.getSize() : 20;
        long total = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 1;

        List<ClientAccount> pageSlice = paginate(all, page, size);

        List<AccountSummary> content = pageSlice.stream()
                .map(acc -> mapToAccountSummary(acc, hasDateFilter ? from : null,
                                                    hasDateFilter ? to   : null))
                .collect(Collectors.toList());

        // ── Totales globales (sobre TODOS los registros, no solo la página) ──
        BigDecimal totalDebtGlobal = all.stream()
                .map(a -> a.getTotalDebt() != null ? a.getTotalDebt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaidGlobal = all.stream()
                .map(a -> a.getTotalPaid() != null ? a.getTotalPaid() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPendingGlobal = all.stream()
                .filter(a -> a.getCurrentBalance() != null && a.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(ClientAccount::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingCountGlobal = all.stream()
                .filter(a -> a.getCurrentBalance() != null && a.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0)
                .count();

        return PagedAccountReport.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .totalDebtGlobal(totalDebtGlobal)
                .totalPaidGlobal(totalPaidGlobal)
                .totalPendingGlobal(totalPendingGlobal)
                .pendingCountGlobal(pendingCountGlobal)
                .build();
    }

    /**
     * Obtiene todas las cuentas que coinciden con el filtro.
     * Para onlyWithBalance=true usa el @Query probado del repositorio (evita el mismatch
     * de tipos Decimal128 que hace fallar la comparación $gt:0 en MongoTemplate).
     */
    private List<ClientAccount> fetchAccounts(AccountReportFilter filter,
                                              boolean hasDateFilter,
                                              boolean onlyWithBalance,
                                              LocalDateTime from,
                                              LocalDateTime to) {
        if (onlyWithBalance) {
            // @Query("{ 'currentBalance': { $gt: 0 } }") — probado y funcionando
            List<ClientAccount> withBalance = new ArrayList<>(clientAccountRepository.findAllWithBalance(BigDecimal.ZERO));
            log.info("fetchAccounts (onlyWithBalance=true) found {} accounts with balance", withBalance.size());

            // Filtrar por cliente específico si se proporcionó
            if (filter.getClientId() != null && !filter.getClientId().isEmpty()) {
                String cid = filter.getClientId();
                withBalance = withBalance.stream()
                        .filter(a -> cid.equals(a.getClientId()))
                        .collect(Collectors.toList());
            }
            return withBalance;
        }

        // Ruta normal: MongoTemplate con filtros de fecha y cliente
        List<Criteria> andCriteria = new ArrayList<>();

        // Solo saldados: currentBalance = 0 pero con deuda histórica
        if (Boolean.TRUE.equals(filter.getOnlySettled())) {
            andCriteria.add(Criteria.where("totalDebt").gt(BigDecimal.ZERO));
            // currentBalance = 0 se filtra en memoria (Decimal128 mismatch issue)
        }

        if (filter.getClientId() != null && !filter.getClientId().isEmpty()) {
            andCriteria.add(Criteria.where("clientId").is(filter.getClientId()));
        }

        if (hasDateFilter) {
            Criteria byLastPayment = Criteria.where("lastPaymentDate");
            if (from != null) byLastPayment = byLastPayment.gte(from);
            if (to   != null) byLastPayment = byLastPayment.lte(to);

            Criteria byCreatedAt = Criteria.where("createdAt");
            if (from != null) byCreatedAt = byCreatedAt.gte(from);
            if (to   != null) byCreatedAt = byCreatedAt.lte(to);

            andCriteria.add(new Criteria().orOperator(byLastPayment, byCreatedAt));
        }

        Query query = new Query();
        if (!andCriteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(andCriteria.toArray(new Criteria[0])));
        }

        log.info("fetchAccounts MongoDB query: {}", query.getQueryObject().toJson());
        List<ClientAccount> accounts = mongoTemplate.find(query, ClientAccount.class);
        log.info("fetchAccounts found {} accounts before settled filter", accounts.size());

        // Post-filter en memoria para onlySettled (evita mismatch Decimal128)
        if (Boolean.TRUE.equals(filter.getOnlySettled())) {
            accounts = accounts.stream()
                    .filter(a -> a.getCurrentBalance() != null
                              && a.getCurrentBalance().compareTo(BigDecimal.ZERO) == 0)
                    .collect(Collectors.toList());
            log.info("fetchAccounts (onlySettled) reduced to {} accounts", accounts.size());
        }

        return accounts;
    }

    /** Retorna la sublist correspondiente a la página solicitada. */
    private List<ClientAccount> paginate(List<ClientAccount> all, int page, int size) {
        if (size <= 0) return all;
        int fromIdx = page * size;
        if (fromIdx >= all.size()) return List.of();
        int toIdx = Math.min(fromIdx + size, all.size());
        return all.subList(fromIdx, toIdx);
    }

    /**
     * Versión sin filtro de fecha — para llamadas que no son de reporte general.
     */
    private AccountSummary mapToAccountSummary(ClientAccount account) {
        return mapToAccountSummary(account, null, null);
    }

    /**
     * Construye el AccountSummary calculando saldo corrido por pago.
     * Si fromFilter/toFilter están definidos, filtra los pagos mostrados en el detalle
     * (pero los totales reflejan SIEMPRE el estado real de la cuenta).
     */
    private AccountSummary mapToAccountSummary(ClientAccount account,
                                               LocalDateTime fromFilter,
                                               LocalDateTime toFilter) {
        Long daysSinceLastPayment = null;
        if (account.getLastPaymentDate() != null) {
            daysSinceLastPayment = ChronoUnit.DAYS.between(account.getLastPaymentDate(), LocalDateTime.now());
        }

        Client client = account.getClient();
        String clientName      = client != null ? client.getFullName()  : "N/A";
        String clientIdNumber  = client != null ? client.getIdNumber()  : "N/A";
        String clientPhone     = client != null ? client.getPhone()     : null;
        String clientAddress   = client != null ? client.getAddress()   : null;
        String clientNickname  = client != null ? client.getNickname()  : null;

        // ── Historial de pagos con saldo corrido (ordenado del más reciente al más antiguo) ──
        // El saldo corrido se calcula ASC para que los números sean correctos,
        // luego la lista resultante se invierte para mostrar del último al primero.
        List<AccountSummary.PaymentWithBalance> paymentsWithBalance = new ArrayList<>();
        if (account.getPayments() != null && !account.getPayments().isEmpty()) {
            List<AccountPayment> sortedAsc = account.getPayments().stream()
                    .filter(p -> p.getPaymentDate() != null)
                    .sorted(Comparator.comparing(AccountPayment::getPaymentDate))
                    .collect(Collectors.toList());

            BigDecimal runningBalance = account.getTotalDebt() != null
                    ? account.getTotalDebt() : BigDecimal.ZERO;

            for (AccountPayment payment : sortedAsc) {
                BigDecimal amount = payment.getAmount() != null
                        ? payment.getAmount() : BigDecimal.ZERO;
                BigDecimal before = runningBalance;
                BigDecimal after  = runningBalance.subtract(amount);
                runningBalance = after;

                // Aplicar filtro de fecha al detalle si corresponde
                LocalDateTime pd = payment.getPaymentDate();
                boolean inRange = (fromFilter == null || !pd.isBefore(fromFilter))
                               && (toFilter   == null || !pd.isAfter(toFilter));
                if (inRange) {
                    paymentsWithBalance.add(AccountSummary.PaymentWithBalance.builder()
                            .id(payment.getId())
                            .amount(amount)
                            .paymentMethod(payment.getPaymentMethod() != null
                                    ? payment.getPaymentMethod().name() : null)
                            .bankAccountName(payment.getBankAccountName())
                            .reference(payment.getReference())
                            .notes(payment.getNotes())
                            .paymentDate(payment.getPaymentDate())
                            .createdBy(payment.getCreatedBy())
                            .balanceBefore(before)
                            .balanceAfter(after)
                            .build());
                }
            }
            // Invertir para mostrar del más reciente al más antiguo
            Collections.reverse(paymentsWithBalance);
        }

        // ── Facturas a crédito ────────────────────────────────────────────────
        Query billingsQuery = new Query();
        billingsQuery.addCriteria(Criteria.where("client.id").is(account.getClientId()));
        billingsQuery.addCriteria(Criteria.where("saleType").is(EPaymentType.CREDITO));
        billingsQuery.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "dateTimeRecord"));
        List<Billing> billings = mongoTemplate.find(billingsQuery, Billing.class);
        List<BillingDto> creditBillings = billingMapper.mapToDtoList(billings);

        // ── Líneas planas para el PDF (cabecera de factura + productos) ─────────
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<AccountSummary.BillingDetailLine> billingDetailLines = new ArrayList<>();
        if (creditBillings != null) {
            for (BillingDto billing : creditBillings) {
                // Fila HEADER con datos de la factura
                billingDetailLines.add(AccountSummary.BillingDetailLine.builder()
                        .rowType("HEADER")
                        .billNumber(billing.getBillNumber() != null
                                ? billing.getBillNumber()
                                : (billing.getId() != null ? billing.getId().substring(0, Math.min(8, billing.getId().length())) : "-"))
                        .billingDate(billing.getDateTimeRecord() != null
                                ? billing.getDateTimeRecord().format(dtf)
                                : "-")
                        .billTotal(billing.getTotalBilling())
                        .build());
                // Filas PRODUCT — una por cada ítem de la factura
                if (billing.getSaleDetails() != null && !billing.getSaleDetails().isEmpty()) {
                    billing.getSaleDetails().forEach(detail ->
                            billingDetailLines.add(AccountSummary.BillingDetailLine.builder()
                                    .rowType("PRODUCT")
                                    .productDescription(detail.getProduct() != null
                                            ? detail.getProduct().getDescription() : "-")
                                    .quantity(detail.getAmount())
                                    .unitPrice(detail.getUnitPrice())
                                    .subtotal(detail.getSubTotal())
                                    .build()));
                }
            }
        }

        // ── Transacciones manuales (cuaderno, ajustes, devoluciones) ──────────
        List<AccountSummary.ManualTransaction> manualTx = new ArrayList<>();
        if (account.getTransactions() != null) {
            account.getTransactions().stream()
                .filter(t -> t.getType() == EAccountTransactionType.MANUAL_DEBT
                          || t.getType() == EAccountTransactionType.ADJUSTMENT
                          || t.getType() == EAccountTransactionType.RETURN_ADJUSTMENT)
                .forEach(t -> manualTx.add(AccountSummary.ManualTransaction.builder()
                    .id(t.getId())
                    .type(t.getType().name())
                    .amount(t.getAmount())
                    .balanceAfter(t.getBalanceAfter())
                    .notes(t.getNotes())
                    .source(t.getSource())
                    .transactionDate(t.getTransactionDate())
                    .createdBy(t.getCreatedBy())
                    .createdAt(t.getCreatedAt())
                    .build()));
        }

        return AccountSummary.builder()
                .clientId(account.getClientId())
                .clientName(clientName)
                .clientIdNumber(clientIdNumber)
                .clientPhone(clientPhone)
                .clientAddress(clientAddress)
                .clientNickname(clientNickname)
                .totalDebt(account.getTotalDebt())
                .totalPaid(account.getTotalPaid())
                .currentBalance(account.getCurrentBalance())
                .lastPaymentDate(account.getLastPaymentDate())
                .daysSinceLastPayment(daysSinceLastPayment)
                .payments(paymentsWithBalance)
                .creditBillings(creditBillings)
                .billingDetailLines(billingDetailLines)
                .manualTransactions(manualTx)
                .build();
    }

    private ClientAccount createNewAccount(String clientId) {
        log.info("Creating new ClientAccount for clientId: {}", clientId);
        
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        ClientAccount account = ClientAccount.builder()
                .clientId(clientId)
                .client(client)
                .totalDebt(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .payments(new ArrayList<>())
                .transactions(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return clientAccountRepository.save(account);
    }

    @Override
    @Transactional
    public AccountTransaction registerManualDebt(ManualDebtRequest request, String createdBy) {
        log.info("ClientAccountServiceImpl -> registerManualDebt: clientId={}, amount={}", 
                request.getClientId(), request.getAmount());

        if (request.getClientId() == null || request.getClientId().isEmpty()) {
            throw new IllegalArgumentException("El ID del cliente es requerido");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }

        if (request.getTransactionDate() == null) {
            throw new IllegalArgumentException("La fecha de transacción es requerida");
        }

        if (request.getNotes() == null || request.getNotes().isEmpty()) {
            throw new IllegalArgumentException("La descripción es requerida");
        }

        ClientAccount account = clientAccountRepository.findByClientId(request.getClientId())
                .orElseGet(() -> createNewAccount(request.getClientId()));

        account.setTotalDebt(account.getTotalDebt().add(request.getAmount()));
        account.setCurrentBalance(account.getTotalDebt().subtract(account.getTotalPaid()));
        account.setUpdatedAt(LocalDateTime.now());

        AccountTransaction transaction = AccountTransaction.builder()
                .id(UUID.randomUUID().toString())
                .type(EAccountTransactionType.MANUAL_DEBT)
                .amount(request.getAmount())
                .balanceAfter(account.getCurrentBalance())
                .notes(request.getNotes())
                .source(request.getSource())
                .transactionDate(request.getTransactionDate())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        account.getTransactions().add(transaction);
        clientAccountRepository.save(account);
        
        log.info("Manual debt registered successfully. New balance: {}", account.getCurrentBalance());
        return transaction;
    }

    @Override
    @Transactional
    public AccountTransaction reduceDebtForReturn(String clientId, BigDecimal amount, String returnId,
                                                  String notes, String createdBy) {
        log.info("ClientAccountServiceImpl -> reduceDebtForReturn: clientId={}, amount={}, returnId={}",
                clientId, amount, returnId);

        ClientAccount account = clientAccountRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada para el cliente: " + clientId));

        BigDecimal reduction = amount.min(account.getTotalDebt());
        account.setTotalDebt(account.getTotalDebt().subtract(reduction));
        account.setCurrentBalance(account.getTotalDebt().subtract(account.getTotalPaid()));
        account.setUpdatedAt(LocalDateTime.now());

        AccountTransaction transaction = AccountTransaction.builder()
                .id(UUID.randomUUID().toString())
                .type(EAccountTransactionType.RETURN_ADJUSTMENT)
                .amount(reduction.negate())
                .balanceAfter(account.getCurrentBalance())
                .billingId(returnId)
                .notes(notes != null ? notes : "Ajuste por devolución de mercancía")
                .source("DEVOLUCION-" + returnId)
                .transactionDate(java.time.LocalDate.now())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        account.getTransactions().add(transaction);
        clientAccountRepository.save(account);

        log.info("Debt reduced for return. New balance: {}", account.getCurrentBalance());
        return transaction;
    }
}
