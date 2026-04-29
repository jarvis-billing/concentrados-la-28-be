package com.co.jarvis.dto.cashregister;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBankReconciliationRequest implements Serializable {

    private LocalDate sessionDate;
    private String bankAccountId;
    private BigDecimal openingBalance;
    private BigDecimal totalBankCounted;
    private String notes;
}
