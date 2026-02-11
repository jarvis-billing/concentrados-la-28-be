package com.co.jarvis.service.impl;

import com.co.jarvis.dto.AccountReportFilter;
import com.co.jarvis.dto.AccountSummary;
import com.co.jarvis.dto.BillingDto;
import com.co.jarvis.dto.ManualDebtRequest;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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
        return clientAccountRepository.findAllWithBalance();
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

        AccountPayment payment = AccountPayment.builder()
                .id(UUID.randomUUID().toString())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
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
        log.info("ClientAccountServiceImpl -> generateReport");

        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.getClientId() != null && !filter.getClientId().isEmpty()) {
            criteriaList.add(Criteria.where("clientId").is(filter.getClientId()));
        }

        if (Boolean.TRUE.equals(filter.getOnlyWithBalance())) {
            criteriaList.add(Criteria.where("currentBalance").gt(0));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        List<ClientAccount> accounts = mongoTemplate.find(query, ClientAccount.class);

        return accounts.stream()
                .filter(account -> filterByDateRange(account, filter))
                .map(this::mapToAccountSummary)
                .collect(Collectors.toList());
    }

    private boolean filterByDateRange(ClientAccount account, AccountReportFilter filter) {
        if (filter.getFromDate() == null && filter.getToDate() == null) {
            return true;
        }

        LocalDateTime createdAt = account.getCreatedAt();
        if (createdAt == null) {
            return true;
        }

        if (filter.getFromDate() != null && createdAt.isBefore(filter.getFromDate().atStartOfDay())) {
            return false;
        }

        if (filter.getToDate() != null && createdAt.isAfter(filter.getToDate().atTime(LocalTime.MAX))) {
            return false;
        }

        return true;
    }

    private AccountSummary mapToAccountSummary(ClientAccount account) {
        Long daysSinceLastPayment = null;
        if (account.getLastPaymentDate() != null) {
            daysSinceLastPayment = ChronoUnit.DAYS.between(account.getLastPaymentDate(), LocalDateTime.now());
        }

        Client client = account.getClient();
        String clientName = client != null ? client.getFullName() : "N/A";
        String clientIdNumber = client != null ? client.getIdNumber() : "N/A";

        return AccountSummary.builder()
                .clientId(account.getClientId())
                .clientName(clientName)
                .clientIdNumber(clientIdNumber)
                .totalDebt(account.getTotalDebt())
                .totalPaid(account.getTotalPaid())
                .currentBalance(account.getCurrentBalance())
                .lastPaymentDate(account.getLastPaymentDate())
                .daysSinceLastPayment(daysSinceLastPayment)
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
}
