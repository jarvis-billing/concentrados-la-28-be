package com.co.jarvis.repository;

import com.co.jarvis.entity.CashCountSession;
import com.co.jarvis.enums.ECashCountStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CashCountSessionRepository extends MongoRepository<CashCountSession, String> {

    Optional<CashCountSession> findBySessionDate(LocalDate sessionDate);

    @Query("{ 'sessionDate': { $gte: ?0, $lte: ?1 } }")
    List<CashCountSession> findBySessionDateBetween(LocalDate fromDate, LocalDate toDate);

    @Query("{ 'sessionDate': { $gte: ?0, $lte: ?1 }, 'status': ?2 }")
    List<CashCountSession> findBySessionDateBetweenAndStatus(LocalDate fromDate, LocalDate toDate, ECashCountStatus status);

    List<CashCountSession> findByStatus(ECashCountStatus status);

    @Query(value = "{ 'status': 'CERRADO' }", sort = "{ 'sessionDate': -1 }")
    List<CashCountSession> findLastClosedSession();

    boolean existsBySessionDate(LocalDate sessionDate);
}
