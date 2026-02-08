package com.co.jarvis.dto.cashregister;

import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.enums.ETransactionCategory;
import com.co.jarvis.enums.ETransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashTransactionDto implements Serializable {
    private String id;
    private ETransactionType type;
    private ETransactionCategory category;
    private String description;
    private BigDecimal amount;
    private EPaymentMethod paymentMethod;
    private String reference;
    private LocalDateTime transactionDate;
    private String relatedDocumentId;
}
