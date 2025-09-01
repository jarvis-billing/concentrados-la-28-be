package com.co.jarvis.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Document(collection = "PERSONS")
public class Person implements Serializable {

    @Id
    private String id;
    private String documentType;
    private String documentNumber;
    private String name;
    private String cellPhone;
    private String sex;
    private String address;
    private String mail;
}
