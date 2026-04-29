package com.co.jarvis.repository;

import com.co.jarvis.entity.BankAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends MongoRepository<BankAccount, String> {

    List<BankAccount> findByActiveTrue();

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndActiveTrue(String accountNumber);
}
