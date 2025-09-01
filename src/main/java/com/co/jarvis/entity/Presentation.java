package com.co.jarvis.entity;

import com.co.jarvis.enums.UnitMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Presentation {
    private String barcode;
    private String productCode;
    private String label;
    private BigDecimal weightKg;
    private BigDecimal price;
    private BigDecimal cost;
    private UnitMeasure unitMeasure;
}
