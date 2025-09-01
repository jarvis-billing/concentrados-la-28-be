package com.co.jarvis.dto;

import com.co.jarvis.enums.EVat;
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
public class ProductVatTypeDto implements Serializable {

    private String id;
    private EVat vatType;
    private BigDecimal percentage;
}
