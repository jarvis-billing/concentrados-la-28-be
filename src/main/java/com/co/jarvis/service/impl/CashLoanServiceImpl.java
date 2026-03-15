package com.co.jarvis.service.impl;

import com.co.jarvis.dto.cashregister.CashLoanDto;
import com.co.jarvis.dto.cashregister.CreateCashLoanRequest;
import com.co.jarvis.dto.cashregister.ReturnCashLoanRequest;
import com.co.jarvis.entity.CashLoan;
import com.co.jarvis.enums.ECashLoanStatus;
import com.co.jarvis.repository.CashLoanRepository;
import com.co.jarvis.service.CashLoanService;
import com.co.jarvis.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashLoanServiceImpl implements CashLoanService {

    private final CashLoanRepository cashLoanRepository;

    @Override
    public CashLoanDto create(CreateCashLoanRequest request, String createdBy) {
        log.info("CashLoanServiceImpl -> create: borrower={}, amount={}", request.getBorrower(), request.getAmount());

        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto del préstamo debe ser mayor a cero");
        }
        if (request.getBorrower() == null || request.getBorrower().isBlank()) {
            throw new RuntimeException("Debe especificar quién recibe el préstamo");
        }
        if (request.getLoanDate() == null) {
            throw new RuntimeException("Debe especificar la fecha del préstamo");
        }

        CashLoan loan = CashLoan.builder()
                .loanDate(request.getLoanDate())
                .amount(request.getAmount())
                .borrower(request.getBorrower())
                .reason(request.getReason())
                .notes(request.getNotes())
                .status(ECashLoanStatus.PENDIENTE)
                .createdBy(createdBy)
                .createdAt(DateTimeUtil.nowLocalDateTime())
                .build();

        loan = cashLoanRepository.save(loan);
        log.info("CashLoanServiceImpl -> create -> Préstamo creado con ID: {}", loan.getId());
        return mapToDto(loan);
    }

    @Override
    public CashLoanDto registerReturn(String id, ReturnCashLoanRequest request, String updatedBy) {
        log.info("CashLoanServiceImpl -> registerReturn: id={}", id);

        CashLoan loan = cashLoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado con ID: " + id));

        if (loan.getStatus() != ECashLoanStatus.PENDIENTE) {
            throw new RuntimeException("Solo se puede registrar devolución de préstamos en estado PENDIENTE. Estado actual: " + loan.getStatus());
        }
        if (request.getReturnedAmount() == null || request.getReturnedAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto devuelto debe ser mayor a cero");
        }
        if (request.getReturnedAmount().compareTo(loan.getAmount()) > 0) {
            throw new RuntimeException("El monto devuelto no puede ser mayor al monto prestado");
        }

        loan.setStatus(ECashLoanStatus.DEVUELTO);
        loan.setReturnDate(request.getReturnDate() != null ? request.getReturnDate() : LocalDate.now());
        loan.setReturnedAmount(request.getReturnedAmount());
        loan.setReturnNotes(request.getReturnNotes());
        loan.setUpdatedBy(updatedBy);
        loan.setUpdatedAt(DateTimeUtil.nowLocalDateTime());

        loan = cashLoanRepository.save(loan);
        log.info("CashLoanServiceImpl -> registerReturn -> Devolución registrada para préstamo: {}", id);
        return mapToDto(loan);
    }

    @Override
    public CashLoanDto cancel(String id, String updatedBy) {
        log.info("CashLoanServiceImpl -> cancel: id={}", id);

        CashLoan loan = cashLoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado con ID: " + id));

        if (loan.getStatus() == ECashLoanStatus.DEVUELTO) {
            throw new RuntimeException("No se puede anular un préstamo ya devuelto");
        }
        if (loan.getStatus() == ECashLoanStatus.ANULADO) {
            throw new RuntimeException("El préstamo ya está anulado");
        }

        loan.setStatus(ECashLoanStatus.ANULADO);
        loan.setUpdatedBy(updatedBy);
        loan.setUpdatedAt(DateTimeUtil.nowLocalDateTime());

        loan = cashLoanRepository.save(loan);
        log.info("CashLoanServiceImpl -> cancel -> Préstamo anulado: {}", id);
        return mapToDto(loan);
    }

    @Override
    public CashLoanDto getById(String id) {
        log.info("CashLoanServiceImpl -> getById: {}", id);
        CashLoan loan = cashLoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado con ID: " + id));
        return mapToDto(loan);
    }

    @Override
    public List<CashLoanDto> list(LocalDate fromDate, LocalDate toDate, ECashLoanStatus status) {
        log.info("CashLoanServiceImpl -> list: from={}, to={}, status={}", fromDate, toDate, status);

        List<CashLoan> loans;
        if (fromDate != null && toDate != null && status != null) {
            loans = cashLoanRepository.findByLoanDateBetweenAndStatus(fromDate, toDate, status);
        } else if (fromDate != null && toDate != null) {
            loans = cashLoanRepository.findByLoanDateBetween(fromDate, toDate);
        } else if (status != null) {
            loans = cashLoanRepository.findByStatus(status);
        } else {
            loans = cashLoanRepository.findAll();
        }

        return loans.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private CashLoanDto mapToDto(CashLoan loan) {
        return CashLoanDto.builder()
                .id(loan.getId())
                .loanDate(loan.getLoanDate())
                .amount(loan.getAmount())
                .borrower(loan.getBorrower())
                .reason(loan.getReason())
                .notes(loan.getNotes())
                .status(loan.getStatus())
                .returnDate(loan.getReturnDate())
                .returnedAmount(loan.getReturnedAmount())
                .returnNotes(loan.getReturnNotes())
                .createdBy(loan.getCreatedBy())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }
}
