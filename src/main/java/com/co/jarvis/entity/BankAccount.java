package com.co.jarvis.entity;

import com.co.jarvis.enums.EBankAccount;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una cuenta bancaria de la empresa.
 * Permite conciliar cada cuenta de forma independiente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "BANK_ACCOUNTS")
public class BankAccount {

    @Id
    private String id;

    private String name;

    @Field("bank_name")
    private String bankName;

    @Field("account_number")
    private String accountNumber;

    @Field("account_type")
    private EBankAccount accountType;

    @Builder.Default
    private boolean active = true;

    private String notes;

    @Builder.Default
    @Field("audit_trail")
    private List<AuditEntry> auditTrail = new ArrayList<>();

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
