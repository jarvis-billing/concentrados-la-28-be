package com.co.jarvis.service.impl;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.transfer.CashToBankTransferRequest;
import com.co.jarvis.dto.transfer.InternalTransferDto;
import com.co.jarvis.entity.InternalTransfer;
import com.co.jarvis.enums.EInternalTransferStatus;
import com.co.jarvis.enums.EInternalTransferType;
import com.co.jarvis.repository.InternalTransferRepository;
import com.co.jarvis.service.CashRegisterService;
import com.co.jarvis.service.InternalTransferService;
import com.co.jarvis.util.DateTimeUtil;
import com.co.jarvis.util.exception.InsufficientFundsException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalTransferServiceImpl implements InternalTransferService {

    private static final String DEFAULT_SOURCE_ID = "MAIN_CASH_REGISTER";

    private final InternalTransferRepository internalTransferRepository;
    private final CashRegisterService cashRegisterService;

    @Override
    @Transactional
    public InternalTransferDto transferCashToBank(CashToBankTransferRequest request, UserDto user) {
        log.info("InternalTransferServiceImpl -> transferCashToBank: amount={}, bankAccountId={}",
                request != null ? request.getAmount() : null,
                request != null ? request.getBankAccountId() : null);

        validateCashToBankRequest(request);

        LocalDate transferDate = request.getTransferDate() != null
                ? request.getTransferDate()
                : LocalDate.now(DateTimeUtil.getBogotaZone());

        // Validar saldo disponible en caja ANTES de registrar el movimiento
        BigDecimal availableCash = cashRegisterService.getCurrentCashBalance(transferDate);
        if (request.getAmount().compareTo(availableCash) > 0) {
            log.warn("Intento de traslado rechazado por saldo insuficiente. Solicitado={}, disponible={}",
                    request.getAmount(), availableCash);
            throw new InsufficientFundsException(request.getAmount(), availableCash);
        }

        InternalTransfer transfer = InternalTransfer.builder()
                .transferDate(transferDate)
                .transferDateTime(DateTimeUtil.nowLocalDateTime())
                .amount(request.getAmount())
                .type(EInternalTransferType.TRASLADO_EFECTIVO_BANCO)
                .sourceId(DEFAULT_SOURCE_ID)
                .destinationBankAccountId(request.getBankAccountId())
                .destinationBankName(request.getBankName())
                .destinationAccountNumber(request.getAccountNumber())
                .destinationAccountType(request.getAccountType())
                .responsibleUserId(user != null ? user.getNumberIdentity() : null)
                .responsibleUserName(user != null ? user.getFullName() : null)
                .reference(request.getReference())
                .notes(request.getNotes())
                .status(EInternalTransferStatus.ACTIVO)
                .createdAt(DateTimeUtil.nowLocalDateTime())
                .build();

        transfer = internalTransferRepository.save(transfer);
        log.info("Traslado efectivo a banco registrado. id={}, monto={}, banco={}",
                transfer.getId(), transfer.getAmount(), transfer.getDestinationBankName());

        return mapToDto(transfer);
    }

    @Override
    public InternalTransferDto getById(String id) {
        log.info("InternalTransferServiceImpl -> getById: {}", id);
        InternalTransfer transfer = internalTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traslado no encontrado con ID: " + id));
        return mapToDto(transfer);
    }

    @Override
    @Transactional
    public InternalTransferDto cancel(String id, String reason, UserDto user) {
        log.info("InternalTransferServiceImpl -> cancel: id={}, reason={}", id, reason);

        InternalTransfer transfer = internalTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traslado no encontrado con ID: " + id));

        if (transfer.getStatus() == EInternalTransferStatus.ANULADO) {
            throw new RuntimeException("El traslado ya está anulado");
        }

        transfer.setStatus(EInternalTransferStatus.ANULADO);
        transfer.setCancelReason(reason);
        transfer.setCancelledAt(DateTimeUtil.nowLocalDateTime());

        transfer = internalTransferRepository.save(transfer);
        log.info("Traslado anulado. id={}, usuario={}",
                transfer.getId(), user != null ? user.getFullName() : "desconocido");

        return mapToDto(transfer);
    }

    @Override
    public List<InternalTransferDto> list(LocalDate fromDate, LocalDate toDate,
                                          EInternalTransferType type,
                                          EInternalTransferStatus status) {
        log.info("InternalTransferServiceImpl -> list: from={}, to={}, type={}, status={}",
                fromDate, toDate, type, status);

        List<InternalTransfer> results;
        if (fromDate != null && toDate != null && status != null) {
            results = internalTransferRepository.findByTransferDateBetweenAndStatus(fromDate, toDate, status);
        } else if (fromDate != null && toDate != null) {
            results = internalTransferRepository.findByTransferDateBetween(fromDate, toDate);
        } else if (status != null) {
            results = internalTransferRepository.findByStatus(status);
        } else {
            results = internalTransferRepository.findAll();
        }

        if (type != null) {
            results = results.stream()
                    .filter(t -> t.getType() == type)
                    .collect(Collectors.toList());
        }

        return results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void validateCashToBankRequest(CashToBankTransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de traslado no puede ser nula");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del traslado debe ser mayor a cero");
        }
        if (request.getBankAccountId() == null || request.getBankAccountId().isBlank()) {
            throw new IllegalArgumentException("Debe especificar la cuenta bancaria de destino");
        }
        if (request.getReference() == null || request.getReference().isBlank()) {
            throw new IllegalArgumentException("Debe especificar el número de comprobante");
        }
    }

    private InternalTransferDto mapToDto(InternalTransfer t) {
        return InternalTransferDto.builder()
                .id(t.getId())
                .transferDate(t.getTransferDate())
                .transferDateTime(t.getTransferDateTime())
                .amount(t.getAmount())
                .type(t.getType())
                .sourceId(t.getSourceId())
                .destinationBankAccountId(t.getDestinationBankAccountId())
                .destinationBankName(t.getDestinationBankName())
                .destinationAccountNumber(t.getDestinationAccountNumber())
                .destinationAccountType(t.getDestinationAccountType())
                .responsibleUserId(t.getResponsibleUserId())
                .responsibleUserName(t.getResponsibleUserName())
                .reference(t.getReference())
                .notes(t.getNotes())
                .status(t.getStatus())
                .cancelledAt(t.getCancelledAt())
                .cancelReason(t.getCancelReason())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
