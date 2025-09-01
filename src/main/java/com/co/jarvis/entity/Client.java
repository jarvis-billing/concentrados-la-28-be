package com.co.jarvis.entity;

import com.co.jarvis.util.Metadata;
import com.co.jarvis.enums.EClient;
import com.co.jarvis.enums.EDocument;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("CLIENTS")
public class Client extends Metadata {

    @Id
    private String id;
    private String idNumber;
    private String name;
    private String surname;
    private String address;
    private String phone;
    private String email;
    private String businessName;
    private boolean autoReportBilling;
    private EClient clientType;
    private EDocument documentType;
    private String nickname;
    private String fullName;

    public String getFullName() {
        if ((name.isBlank() && surname.isBlank()) && !businessName.isBlank()) {
            return businessName;
        }
        return name + " " + surname;
    }

}

