package com.co.jarvis.dto;

import com.co.jarvis.enums.EClient;
import com.co.jarvis.enums.EDocument;
import com.co.jarvis.util.Metadata;
import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientDto extends Metadata implements Serializable {

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

    public void buildFullName() {
        StringBuilder fullNameBuilder = new StringBuilder();

        // Primero, verificamos si el nombre y el apellido están presentes
        if (name != null && !name.isEmpty()) {
            fullNameBuilder.append(name);
        }
        if (surname != null && !surname.isEmpty()) {
            if (!fullNameBuilder.isEmpty()) {
                fullNameBuilder.append(" "); // Añadimos un espacio entre el nombre y el apellido
            }
            fullNameBuilder.append(surname);
        }

        // Si no hay nombre ni apellido, usamos el nickname si está presente
        if (fullNameBuilder.isEmpty() && nickname != null && !nickname.isEmpty()) {
            fullNameBuilder.append(nickname);
        }

        // Si el fullName sigue vacío y tenemos un nombre de empresa, lo añadimos
        if (fullNameBuilder.isEmpty() && businessName != null && !businessName.isEmpty()) {
            fullNameBuilder.append(businessName);
        }

        // Finalmente, asignamos el valor al atributo fullName
        this.fullName = fullNameBuilder.toString();
    }


    public String getFullName() {
        buildFullName();
        return fullName;
    }
}
