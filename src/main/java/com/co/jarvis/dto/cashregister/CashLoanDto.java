package com.co.jarvis.dto.cashregister;

import com.co.jarvis.enums.ECashLoanStatus;
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
public class CashLoanDto implements Serializable {
    private String id;
    private LocalDate loanDate;
    private BigDecimal amount;
    private String borrower;
    private String reason;
    private String notes;
    private ECashLoanStatus status;
    private LocalDate returnDate;
    private BigDecimal returnedAmount;
    private String returnNotes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
