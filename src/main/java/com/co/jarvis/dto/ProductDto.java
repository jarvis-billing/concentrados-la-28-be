package com.co.jarvis.dto;

import com.co.jarvis.enums.EVat;
import com.co.jarvis.enums.ESale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto implements Serializable {

    private String id;
    private String barcode;
    private String description;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal totalValue;
    private ESale saleType;
    private BigDecimal currentStock;
    private BigDecimal minStock;
    private String brand;
    private String pluCode;
    private CategoryDto category;
    private BigDecimal vatValue;
    private EVat vatType;
    private BigDecimal cost;
}
