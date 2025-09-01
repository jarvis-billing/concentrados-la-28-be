package com.co.jarvis.entity;

import com.co.jarvis.enums.EUserRol;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "USERS")
public class User {

    @Id
    private String id;
    private String numberIdentity;
    private String password;
    private String name;
    private String surname;
    private String phone;
    private String address;
    @DBRef(lazy = false)
    private Company company;
    private EUserRol role;
    private String fullName;

    public String getFullName() {
        return name.concat(" ").concat(surname);
    }
}
