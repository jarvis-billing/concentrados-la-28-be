package com.co.jarvis.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "CLIENT_CREDITS")
public class ClientCredit {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field(targetType = FieldType.STRING)
    private String clientId;

    @DBRef
    private Client client;

    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalDeposited = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalUsed = BigDecimal.ZERO;

    private LocalDateTime lastTransactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<CreditTransaction> transactions = new ArrayList<>();

    @Version
    private Long version;
}
