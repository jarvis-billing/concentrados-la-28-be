package com.co.jarvis.dto;

import com.co.jarvis.enums.EBankAccount;
import com.co.jarvis.enums.EBilling;
import com.co.jarvis.enums.EPaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingConfigDto implements Serializable {

    private String id;
    private EBankAccount bankAccountType;
    private EBilling billingType;
    private List<EPaymentMethod> paymentMethods;

    private String resolutionExpiresDate;
    private Long billFrom;
    private Long billUntil;
    private String bank;
    private String bankAccountNumber;
    private String prefixBill;
    private String dianResolutionNumber;
    private String taxRegime;
    private boolean isCurrentResolution;
}
