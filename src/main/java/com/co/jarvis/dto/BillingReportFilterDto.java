package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingReportFilterDto {

    private LocalDate fromDate;

    private LocalDate toDate;

    private String billNumber;

    private String userSale;

    private String client;

    private String clientField;

    private String product;

    private String productField;


    public boolean hasFilterDate() {
        return toDate != null && fromDate != null;
    }
}
