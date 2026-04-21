package com.co.jarvis.dto.transfer;

import com.co.jarvis.enums.EBankAccount;
import com.co.jarvis.enums.EInternalTransferStatus;
import com.co.jarvis.enums.EInternalTransferType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalTransferDto implements Serializable {

    private String id;
    private LocalDate transferDate;
    private LocalDateTime transferDateTime;
    private BigDecimal amount;
    private EInternalTransferType type;
    private String sourceId;
    private String destinationBankName;
    private String destinationAccountNumber;
    private EBankAccount destinationAccountType;
    private String responsibleUserId;
    private String responsibleUserName;
    private String reference;
    private String notes;
    private EInternalTransferStatus status;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private String supportFileName;
    private String supportFileUrl;
}
