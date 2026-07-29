package com.co.jarvis.repository;

import com.co.jarvis.entity.ClientAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ClientAccountRepository extends MongoRepository<ClientAccount, String> {

    Optional<ClientAccount> findByClientId(String clientId);

    /**
     * Parámetro ?0 fuerza serialización como Decimal128, evitando el mismatch
     * de tipos que ocurre cuando se usa el literal 0 (entero) contra Decimal128.
     */
    @Query("{ 'currentBalance': { $gt: ?0 } }")
    List<ClientAccount> findAllWithBalance(BigDecimal minBalance);

    boolean existsByClientId(String clientId);
}
