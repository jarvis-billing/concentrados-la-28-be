package com.co.jarvis.dto.bankaccount;

import com.co.jarvis.enums.EBankAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBankAccountRequest implements Serializable {

    private String name;
    private String bankName;
    private String accountNumber;
    private EBankAccount accountType;
    private String notes;
}
