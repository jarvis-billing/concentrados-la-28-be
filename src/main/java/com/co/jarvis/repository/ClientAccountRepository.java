package com.co.jarvis.repository;

import com.co.jarvis.entity.ClientAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientAccountRepository extends MongoRepository<ClientAccount, String> {

    Optional<ClientAccount> findByClientId(String clientId);

    @Query("{ 'currentBalance': { $gt: 0 } }")
    List<ClientAccount> findAllWithBalance();

    boolean existsByClientId(String clientId);
}
