package com.co.jarvis.dto;

import com.co.jarvis.enums.EUserRol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {

    private String id;
    private String numberIdentity;
    private String password;
    private String name;
    private String surname;
    private String phone;
    private String address;
    private CompanyDto company;
    private EUserRol rol;
    private String fullName;

    public String getFullName() {
        return name.concat(" ").concat(surname);
    }
}
