package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountReportFilter {

    private String clientId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean onlyWithBalance;
    /** Solo clientes cuyo saldo = 0 pero que tuvieron deuda (saldados) */
    private Boolean onlySettled;
    private Integer page;
    private Integer size;
}
