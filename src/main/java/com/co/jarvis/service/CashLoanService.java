package com.co.jarvis.service;

import com.co.jarvis.dto.cashregister.CashLoanDto;
import com.co.jarvis.dto.cashregister.CreateCashLoanRequest;
import com.co.jarvis.dto.cashregister.ReturnCashLoanRequest;
import com.co.jarvis.enums.ECashLoanStatus;

import java.time.LocalDate;
import java.util.List;

public interface CashLoanService {
    CashLoanDto create(CreateCashLoanRequest request, String createdBy);
    CashLoanDto registerReturn(String id, ReturnCashLoanRequest request, String updatedBy);
    CashLoanDto cancel(String id, String updatedBy);
    CashLoanDto getById(String id);
    List<CashLoanDto> list(LocalDate fromDate, LocalDate toDate, ECashLoanStatus status);
}
