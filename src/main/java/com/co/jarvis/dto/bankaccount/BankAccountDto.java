package com.co.jarvis.dto.bankaccount;

import com.co.jarvis.dto.cashregister.AuditEntryDto;
import com.co.jarvis.enums.EBankAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDto implements Serializable {

    private String id;
    private String name;
    private String bankName;
    private String accountNumber;
    private EBankAccount accountType;
    private boolean active;
    private String notes;
    private List<AuditEntryDto> auditTrail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
