package com.co.jarvis.service;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.transfer.CashToBankTransferRequest;
import com.co.jarvis.dto.transfer.InternalTransferDto;
import com.co.jarvis.enums.EInternalTransferStatus;
import com.co.jarvis.enums.EInternalTransferType;

import java.time.LocalDate;
import java.util.List;

public interface InternalTransferService {

    /**
     * Registra una consignación de efectivo desde la caja hacia un banco.
     * Valida que la caja tenga saldo suficiente antes de persistir el movimiento.
     */
    InternalTransferDto transferCashToBank(CashToBankTransferRequest request, UserDto user);

    InternalTransferDto getById(String id);

    /**
     * Anula un traslado previamente registrado. El efectivo "regresa" virtualmente
     * a la caja (el arqueo deja de contarlo como egreso).
     */
    InternalTransferDto cancel(String id, String reason, UserDto user);

    List<InternalTransferDto> list(LocalDate fromDate, LocalDate toDate,
                                    EInternalTransferType type,
                                    EInternalTransferStatus status);
}
