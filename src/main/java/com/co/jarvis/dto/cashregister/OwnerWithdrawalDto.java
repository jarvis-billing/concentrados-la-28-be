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
public class OwnerWithdrawalDto implements Serializable {
    private String id;
    private BigDecimal amount;
    private String description;
    private String reference;
    private LocalDate date;
    private String createdBy;
}
