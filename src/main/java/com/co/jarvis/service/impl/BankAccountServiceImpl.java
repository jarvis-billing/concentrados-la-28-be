package com.co.jarvis.service.impl;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.bankaccount.BankAccountDto;
import com.co.jarvis.dto.bankaccount.CreateBankAccountRequest;
import com.co.jarvis.dto.cashregister.AuditEntryDto;
import com.co.jarvis.entity.AuditEntry;
import com.co.jarvis.entity.BankAccount;
import com.co.jarvis.enums.EAuditAction;
import com.co.jarvis.repository.BankAccountRepository;
import com.co.jarvis.service.BankAccountService;
import com.co.jarvis.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    @Override
    public List<BankAccountDto> listActive() {
        return bankAccountRepository.findByActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BankAccountDto> listAll() {
        return bankAccountRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public BankAccountDto getById(String id) {
        return bankAccountRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Cuenta bancaria no encontrada: " + id));
    }

    @Override
    public BankAccountDto create(CreateBankAccountRequest request, UserDto user) {
        AuditEntry audit = AuditEntry.builder()
                .userId(user != null ? user.getNumberIdentity() : null)
                .userName(user != null ? user.getFullName() : null)
                .action(EAuditAction.APERTURA)
                .timestamp(DateTimeUtil.nowLocalDateTime())
                .build();

        BankAccount account = BankAccount.builder()
                .name(request.getName())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .accountType(request.getAccountType())
                .notes(request.getNotes())
                .active(true)
                .createdAt(DateTimeUtil.nowLocalDateTime())
                .updatedAt(DateTimeUtil.nowLocalDateTime())
                .build();
        account.getAuditTrail().add(audit);

        account = bankAccountRepository.save(account);
        log.info("BankAccountServiceImpl -> create: saved id={}, user={}", account.getId(),
                user != null ? user.getFullName() : "desconocido");
        return mapToDto(account);
    }

    @Override
    public BankAccountDto update(String id, CreateBankAccountRequest request, UserDto user) {
        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta bancaria no encontrada: " + id));

        account.setName(request.getName());
        account.setBankName(request.getBankName());
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountType(request.getAccountType());
        account.setNotes(request.getNotes());
        account.setUpdatedAt(DateTimeUtil.nowLocalDateTime());
        account.getAuditTrail().add(AuditEntry.builder()
                .userId(user != null ? user.getNumberIdentity() : null)
                .userName(user != null ? user.getFullName() : null)
                .action(EAuditAction.ACTUALIZACION)
                .timestamp(DateTimeUtil.nowLocalDateTime())
                .build());

        account = bankAccountRepository.save(account);
        log.info("BankAccountServiceImpl -> update: id={}, user={}", id,
                user != null ? user.getFullName() : "desconocido");
        return mapToDto(account);
    }

    @Override
    public void deactivate(String id, UserDto user) {
        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta bancaria no encontrada: " + id));
        account.setActive(false);
        account.setUpdatedAt(DateTimeUtil.nowLocalDateTime());
        account.getAuditTrail().add(AuditEntry.builder()
                .userId(user != null ? user.getNumberIdentity() : null)
                .userName(user != null ? user.getFullName() : null)
                .action(EAuditAction.ANULACION)
                .timestamp(DateTimeUtil.nowLocalDateTime())
                .details("Cuenta desactivada")
                .build());
        bankAccountRepository.save(account);
        log.info("BankAccountServiceImpl -> deactivate: id={}, user={}", id,
                user != null ? user.getFullName() : "desconocido");
    }

    private BankAccountDto mapToDto(BankAccount account) {
        List<AuditEntryDto> auditDtos = account.getAuditTrail() == null ? List.of() :
                account.getAuditTrail().stream()
                        .map(a -> AuditEntryDto.builder()
                                .userId(a.getUserId())
                                .userName(a.getUserName())
                                .action(a.getAction())
                                .timestamp(a.getTimestamp())
                                .details(a.getDetails())
                                .build())
                        .collect(Collectors.toList());

        return BankAccountDto.builder()
                .id(account.getId())
                .name(account.getName())
                .bankName(account.getBankName())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .active(account.isActive())
                .notes(account.getNotes())
                .auditTrail(auditDtos)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
