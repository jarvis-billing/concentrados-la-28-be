package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseFilterDto implements Serializable {
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;
    private String supplierId;
    private String supplierName;
    private String invoiceNumber;
}
