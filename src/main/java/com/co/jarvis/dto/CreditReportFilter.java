package com.co.jarvis.dto;

import com.co.jarvis.enums.ECreditTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditReportFilter {

    private String clientId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private ECreditTransactionType transactionType;
    private Boolean onlyWithBalance;
}
