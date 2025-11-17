package com.co.jarvis.entity;

import com.co.jarvis.enums.EPaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "SUPPLIER_PAYMENTS")
public class SupplierPayment {
    @Id
    private String id;
    private String supplierId;
    private String supplierName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private EPaymentMethod method;
    private String reference;
    private String notes;
    private String supportUrl; // API URL to download support
    private String supportPath; // local filesystem path
}
