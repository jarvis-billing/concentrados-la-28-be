package com.co.jarvis.service;

import com.co.jarvis.dto.AdjustCreditRequest;
import com.co.jarvis.dto.CreditReportFilter;
import com.co.jarvis.dto.CreditSummary;
import com.co.jarvis.dto.DepositCreditRequest;
import com.co.jarvis.dto.ManualCreditRequest;
import com.co.jarvis.dto.RefundCreditRequest;
import com.co.jarvis.dto.UseCreditRequest;
import com.co.jarvis.entity.ClientCredit;
import com.co.jarvis.entity.CreditTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface ClientCreditService {

    ClientCredit getByClientId(String clientId);

    BigDecimal getClientCreditBalance(String clientId);

    List<ClientCredit> getAllWithBalance();

    List<CreditTransaction> getTransactionsByClientId(String clientId);

    CreditTransaction registerDeposit(DepositCreditRequest request, String createdBy);

    CreditTransaction useCredit(UseCreditRequest request, String createdBy);

    CreditTransaction adjustCredit(AdjustCreditRequest request, String createdBy);

    /**
     * Procesa una devolución de anticipo (saldo a favor) a un cliente.
     * Crea una transacción de tipo REFUND y actualiza el saldo del cliente.
     * También registra una transacción de caja para el arqueo.
     * @param request Datos de la devolución
     * @param createdBy Usuario que procesa la devolución
     * @return Transacción de devolución creada
     */
    CreditTransaction processRefund(RefundCreditRequest request, String createdBy);

    List<CreditSummary> generateReport(CreditReportFilter filter);

    /**
     * Registra un crédito manual para migrar datos del cuaderno físico al sistema.
     * Crea una transacción de tipo DEPOSIT con la fecha original del cuaderno.
     * @param request Datos del crédito manual
     * @param createdBy Usuario que registra el crédito
     * @return Transacción creada
     */
    CreditTransaction registerManualCredit(ManualCreditRequest request, String createdBy);
}
