package com.co.jarvis.entity;

import com.co.jarvis.enums.EVat;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Document("PRODUCT_VAT_TYPE")
public class ProductVatType {

    @Id
    private String id;
    private EVat vatType;
    private BigDecimal percentage;
}
