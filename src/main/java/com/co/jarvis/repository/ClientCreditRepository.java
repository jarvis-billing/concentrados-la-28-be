package com.co.jarvis.repository;

import com.co.jarvis.entity.ClientCredit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientCreditRepository extends MongoRepository<ClientCredit, String> {

    Optional<ClientCredit> findByClientId(String clientId);

    @Query("{ 'currentBalance': { $gt: 0 } }")
    List<ClientCredit> findAllWithBalance();

    boolean existsByClientId(String clientId);
}
