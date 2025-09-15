package com.co.jarvis.entity;

import com.co.jarvis.enums.CatalogType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "CATALOGS")
public class Catalog {

    @Id
    private String id;

    @Indexed(unique = true)
    private String value;    // El valor que se usa en Product

    @Indexed
    private CatalogType type;     // "BRAND" o "CATEGORY"

    private boolean active;

    private LocalDateTime createdAt;

    // Metadata útil
    private int useCount;    // Número de productos que usan este catálogo
}
