package com.co.jarvis.dto;

import com.co.jarvis.enums.CatalogType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CatalogDto implements Serializable {

    private String id;

    private String value;    // El valor que se usa en Product

    private CatalogType type;     // "BRAND" o "CATEGORY"

    private boolean active;

    private LocalDateTime createdAt;

    private int useCount;    // Número de productos que usan este catálogo

}
