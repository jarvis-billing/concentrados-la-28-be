package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingReportFilterPagedDto implements Serializable {

    private String fromDate;
    private String toDate;
    private String billNumber;
    private String userSale;
    private String client;
    private String product;
    private String saleType;
    private String paymentMethod;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
