package com.co.jarvis.entity;

import com.co.jarvis.enums.EStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "COMPANY")
public class Company {

    @Id
    private String id;
    private String nit;
    private String businessName;
    private String phone;
    private String address;
    private String email;
    private EStatus status;
    private BillingConfig billingConfig;
    private String fullDataCompany;


    public String getFullDataCompany() {
        return  "Nit: " + nit +
                "\nTelefono: " + phone +
                "\nDirección: " + address +
                "\nEmail: " + email +
                "\nResolución Dian: " + billingConfig.getDianResolutionNumber() +
                "\nAutorizado Desde: " + billingConfig.getPrefixBill() + "-" + billingConfig.getBillFrom().intValue() +
                " Hasta: " + billingConfig.getPrefixBill() + "-" + billingConfig.getBillUntil();
    }
}
