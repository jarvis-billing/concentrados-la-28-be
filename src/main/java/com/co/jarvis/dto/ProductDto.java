package com.co.jarvis.dto;

import com.co.jarvis.entity.Presentation;
import com.co.jarvis.entity.Stock;
import com.co.jarvis.enums.EVat;
import com.co.jarvis.enums.ESale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto implements Serializable {

    private String id;
    private String description;
    private ESale saleType;
    private String brand;
    private String category;
    private String productCode;
    private List<Presentation> presentations;
    private Stock stock;
    private BigDecimal vatValue;
    private EVat vatType;
    private DisplayStock displayStock;

}
