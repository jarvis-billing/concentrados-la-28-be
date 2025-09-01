package com.co.jarvis.dto;

import com.co.jarvis.enums.EStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDto implements Serializable {

    private String id;
    private String nit;
    private String businessName;
    private String phone;
    private String address;
    private String email;
    private EStatus status;
    private BillingConfigDto billingConfig;
}
