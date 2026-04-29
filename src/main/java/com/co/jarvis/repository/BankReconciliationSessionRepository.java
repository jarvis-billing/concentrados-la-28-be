package com.co.jarvis.repository;

import com.co.jarvis.entity.BankReconciliationSession;
import com.co.jarvis.enums.ECashCountStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankReconciliationSessionRepository extends MongoRepository<BankReconciliationSession, String> {

    Optional<BankReconciliationSession> findBySessionDateAndBankAccountId(LocalDate sessionDate, String bankAccountId);

    @Query("{ 'bankAccountId': ?0, 'sessionDate': { $gte: ?1, $lte: ?2 } }")
    List<BankReconciliationSession> findByBankAccountIdAndSessionDateBetween(String bankAccountId, LocalDate fromDate, LocalDate toDate);

    @Query("{ 'bankAccountId': ?0, 'sessionDate': { $gte: ?1, $lte: ?2 }, 'status': ?3 }")
    List<BankReconciliationSession> findByBankAccountIdAndSessionDateBetweenAndStatus(String bankAccountId, LocalDate fromDate, LocalDate toDate, ECashCountStatus status);

    List<BankReconciliationSession> findByBankAccountIdAndStatus(String bankAccountId, ECashCountStatus status);

    List<BankReconciliationSession> findByBankAccountId(String bankAccountId);

    @Query(value = "{ 'bankAccountId': ?0, 'status': 'CERRADO' }", sort = "{ 'sessionDate': -1 }")
    List<BankReconciliationSession> findLastClosedSessionByBankAccountId(String bankAccountId);

    boolean existsBySessionDateAndBankAccountId(LocalDate sessionDate, String bankAccountId);
}
