package com.co.jarvis.entity;


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
@Document(collection = "CATEGORY")
public class Category {

    @Id
    private String id;
    private String description;
}
