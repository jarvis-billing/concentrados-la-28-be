package com.co.jarvis.repository;

import com.co.jarvis.entity.CashLoan;
import com.co.jarvis.enums.ECashLoanStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CashLoanRepository extends MongoRepository<CashLoan, String> {
    List<CashLoan> findByLoanDate(LocalDate loanDate);
    List<CashLoan> findByStatus(ECashLoanStatus status);
    List<CashLoan> findByLoanDateBetween(LocalDate from, LocalDate to);
    List<CashLoan> findByLoanDateBetweenAndStatus(LocalDate from, LocalDate to, ECashLoanStatus status);
}
