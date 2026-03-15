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
public class CreateCashLoanRequest implements Serializable {
    private LocalDate loanDate;
    private BigDecimal amount;
    private String borrower;
    private String reason;
    private String notes;
}
