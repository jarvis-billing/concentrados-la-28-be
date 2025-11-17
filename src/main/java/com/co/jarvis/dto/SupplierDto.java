package com.co.jarvis.dto;

import com.co.jarvis.enums.DocumentType;
import com.co.jarvis.enums.SupplierStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto implements Serializable {
    private String id;
    private String name;
    private DocumentType documentType;
    private String idNumber;
    private String phone;
    private String email;
    private String address;
    private SupplierStatus status;
}
