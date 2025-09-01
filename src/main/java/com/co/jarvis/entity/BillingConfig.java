package com.co.jarvis.entity;

import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.enums.EBankAccount;
import com.co.jarvis.enums.EBilling;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "BILLING_CONFIG")
public class BillingConfig {

    @Id
    private String id;
    private EBankAccount bankAccountType;
    private EBilling billingType;
    private List<EPaymentMethod> paymentMethods;
    private LocalDate resolutionExpiresDate;
    private Long billFrom;
    private Long billUntil;
    private String bank;
    private String bankAccountNumber;
    private String prefixBill;
    private String dianResolutionNumber;
    private String taxRegime;
    private boolean isCurrentResolution;
}
