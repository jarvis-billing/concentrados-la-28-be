package com.co.jarvis.dto;

import com.co.jarvis.enums.EPaymentMethod;
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
public class SupplierPaymentDto implements Serializable {
    private String id;
    private String supplierId;
    private String supplierName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private EPaymentMethod method;
    private String reference;
    private String notes;
    private String supportUrl;
}
