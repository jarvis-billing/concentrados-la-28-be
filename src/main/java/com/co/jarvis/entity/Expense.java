package com.co.jarvis.entity;

import com.co.jarvis.enums.EPaymentMethod;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "EXPENSES")
public class Expense {

    @Id
    private String id;

    private OffsetDateTime dateTimeRecord;

    private BigDecimal amount;

    private EPaymentMethod paymentMethod;

    private String category;

    private String description;

    private String reference;

    private String source;

    private String createdBy;
}
