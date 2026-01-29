package com.co.jarvis.service.impl;

import com.co.jarvis.dto.AdjustCreditRequest;
import com.co.jarvis.dto.CreditReportFilter;
import com.co.jarvis.dto.CreditSummary;
import com.co.jarvis.dto.DepositCreditRequest;
import com.co.jarvis.dto.UseCreditRequest;
import com.co.jarvis.entity.Client;
import com.co.jarvis.entity.ClientCredit;
import com.co.jarvis.entity.CreditTransaction;
import com.co.jarvis.enums.ECreditTransactionType;
import com.co.jarvis.repository.ClientCreditRepository;
import com.co.jarvis.repository.ClientRepository;
import com.co.jarvis.service.ClientCreditService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCreditServiceImpl implements ClientCreditService {

    private final ClientCreditRepository clientCreditRepository;
    private final ClientRepository clientRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public ClientCredit getByClientId(String clientId) {
        log.info("ClientCreditServiceImpl -> getByClientId: {}", clientId);
        return clientCreditRepository.findByClientId(clientId).orElse(null);
    }

    @Override
    public BigDecimal getClientCreditBalance(String clientId) {
        log.info("ClientCreditServiceImpl -> getClientCreditBalance: {}", clientId);
        return clientCreditRepository.findByClientId(clientId)
                .map(ClientCredit::getCurrentBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public List<ClientCredit> getAllWithBalance() {
        log.info("ClientCreditServiceImpl -> getAllWithBalance");
        return clientCreditRepository.findAllWithBalance();
    }

    @Override
    public List<CreditTransaction> getTransactionsByClientId(String clientId) {
        log.info("ClientCreditServiceImpl -> getTransactionsByClientId: {}", clientId);
        return clientCreditRepository.findByClientId(clientId)
                .map(ClientCredit::getTransactions)
                .orElse(Collections.emptyList());
    }

    @Override
    @Transactional
    public CreditTransaction registerDeposit(DepositCreditRequest request, String createdBy) {
        log.info("ClientCreditServiceImpl -> registerDeposit: clientId={}, amount={}", 
                request.getClientId(), request.getAmount());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto del depósito debe ser mayor a cero");
        }

        ClientCredit credit = clientCreditRepository.findByClientId(request.getClientId())
                .orElseGet(() -> createNewCredit(request.getClientId()));

        BigDecimal newBalance = credit.getCurrentBalance().add(request.getAmount());

        CreditTransaction transaction = CreditTransaction.builder()
                .id(UUID.randomUUID().toString())
                .type(ECreditTransactionType.DEPOSIT)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .paymentMethod(request.getPaymentMethod())
                .reference(request.getReference())
                .notes(request.getNotes())
                .transactionDate(LocalDateTime.now())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        credit.getTransactions().add(transaction);
        credit.setTotalDeposited(credit.getTotalDeposited().add(request.getAmount()));
        credit.setCurrentBalance(newBalance);
        credit.setLastTransactionDate(LocalDateTime.now());
        credit.setUpdatedAt(LocalDateTime.now());

        clientCreditRepository.save(credit);
        log.info("Deposit registered successfully. New balance: {}", credit.getCurrentBalance());

        return transaction;
    }

    @Override
    @Transactional
    public CreditTransaction useCredit(UseCreditRequest request, String createdBy) {
        log.info("ClientCreditServiceImpl -> useCredit: clientId={}, amount={}, billingId={}", 
                request.getClientId(), request.getAmount(), request.getBillingId());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto a usar debe ser mayor a cero");
        }

        ClientCredit credit = clientCreditRepository.findByClientId(request.getClientId())
                .orElseThrow(() -> new RuntimeException("El cliente no tiene saldo a favor"));

        if (request.getAmount().compareTo(credit.getCurrentBalance()) > 0) {
            throw new RuntimeException("El monto excede el saldo a favor disponible");
        }

        BigDecimal newBalance = credit.getCurrentBalance().subtract(request.getAmount());

        CreditTransaction transaction = CreditTransaction.builder()
                .id(UUID.randomUUID().toString())
                .type(ECreditTransactionType.CONSUMPTION)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .billingId(request.getBillingId())
                .notes(request.getNotes())
                .transactionDate(LocalDateTime.now())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        credit.getTransactions().add(transaction);
        credit.setTotalUsed(credit.getTotalUsed().add(request.getAmount()));
        credit.setCurrentBalance(newBalance);
        credit.setLastTransactionDate(LocalDateTime.now());
        credit.setUpdatedAt(LocalDateTime.now());

        clientCreditRepository.save(credit);
        log.info("Credit used successfully. New balance: {}", credit.getCurrentBalance());

        return transaction;
    }

    @Override
    @Transactional
    public CreditTransaction adjustCredit(AdjustCreditRequest request, String createdBy) {
        log.info("ClientCreditServiceImpl -> adjustCredit: clientId={}, amount={}", 
                request.getClientId(), request.getAmount());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("El monto del ajuste no puede ser cero");
        }

        ClientCredit credit = clientCreditRepository.findByClientId(request.getClientId())
                .orElseGet(() -> createNewCredit(request.getClientId()));

        BigDecimal newBalance = credit.getCurrentBalance().add(request.getAmount());

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El ajuste resultaría en un saldo negativo");
        }

        ECreditTransactionType type = request.getAmount().compareTo(BigDecimal.ZERO) > 0 
                ? ECreditTransactionType.ADJUSTMENT 
                : ECreditTransactionType.REFUND;

        CreditTransaction transaction = CreditTransaction.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .amount(request.getAmount().abs())
                .balanceAfter(newBalance)
                .notes(request.getNotes())
                .transactionDate(LocalDateTime.now())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        credit.getTransactions().add(transaction);
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            credit.setTotalDeposited(credit.getTotalDeposited().add(request.getAmount()));
        } else {
            credit.setTotalUsed(credit.getTotalUsed().add(request.getAmount().abs()));
        }
        
        credit.setCurrentBalance(newBalance);
        credit.setLastTransactionDate(LocalDateTime.now());
        credit.setUpdatedAt(LocalDateTime.now());

        clientCreditRepository.save(credit);
        log.info("Credit adjusted successfully. New balance: {}", credit.getCurrentBalance());

        return transaction;
    }

    @Override
    public List<CreditSummary> generateReport(CreditReportFilter filter) {
        log.info("ClientCreditServiceImpl -> generateReport");

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

        List<ClientCredit> credits = mongoTemplate.find(query, ClientCredit.class);

        return credits.stream()
                .filter(credit -> filterByDateRange(credit, filter))
                .filter(credit -> filterByTransactionType(credit, filter))
                .map(this::mapToCreditSummary)
                .collect(Collectors.toList());
    }

    private boolean filterByDateRange(ClientCredit credit, CreditReportFilter filter) {
        if (filter.getFromDate() == null && filter.getToDate() == null) {
            return true;
        }

        LocalDateTime createdAt = credit.getCreatedAt();
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

    private boolean filterByTransactionType(ClientCredit credit, CreditReportFilter filter) {
        if (filter.getTransactionType() == null) {
            return true;
        }

        return credit.getTransactions().stream()
                .anyMatch(t -> t.getType() == filter.getTransactionType());
    }

    private CreditSummary mapToCreditSummary(ClientCredit credit) {
        Client client = credit.getClient();
        String clientName = client != null ? client.getFullName() : "N/A";
        String clientIdNumber = client != null ? client.getIdNumber() : "N/A";

        return CreditSummary.builder()
                .clientId(credit.getClientId())
                .clientName(clientName)
                .clientIdNumber(clientIdNumber)
                .currentBalance(credit.getCurrentBalance())
                .totalDeposited(credit.getTotalDeposited())
                .totalUsed(credit.getTotalUsed())
                .lastTransactionDate(credit.getLastTransactionDate())
                .build();
    }

    private ClientCredit createNewCredit(String clientId) {
        log.info("Creating new ClientCredit for clientId: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        ClientCredit credit = ClientCredit.builder()
                .clientId(clientId)
                .client(client)
                .currentBalance(BigDecimal.ZERO)
                .totalDeposited(BigDecimal.ZERO)
                .totalUsed(BigDecimal.ZERO)
                .transactions(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return clientCreditRepository.save(credit);
    }
}
