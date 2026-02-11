package com.co.jarvis.service;

import com.co.jarvis.dto.AccountReportFilter;
import com.co.jarvis.dto.AccountSummary;
import com.co.jarvis.dto.BillingDto;
import com.co.jarvis.dto.ManualDebtRequest;
import com.co.jarvis.dto.RegisterPaymentRequest;
import com.co.jarvis.entity.AccountPayment;
import com.co.jarvis.entity.AccountTransaction;
import com.co.jarvis.entity.ClientAccount;

import java.math.BigDecimal;
import java.util.List;

public interface ClientAccountService {

    ClientAccount getByClientId(String clientId);

    BigDecimal getClientBalance(String clientId);

    List<ClientAccount> getAllWithBalance();

    List<AccountPayment> getPaymentsByClientId(String clientId);

    List<BillingDto> getCreditBillingsByClientId(String clientId);

    void addDebt(String clientId, BigDecimal amount);

    AccountPayment registerPayment(RegisterPaymentRequest request, String createdBy);

    List<AccountSummary> generateReport(AccountReportFilter filter);

    AccountTransaction registerManualDebt(ManualDebtRequest request, String createdBy);
}
