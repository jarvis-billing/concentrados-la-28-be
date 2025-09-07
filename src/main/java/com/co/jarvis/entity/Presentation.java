package com.co.jarvis.entity;

import com.co.jarvis.enums.UnitMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Presentation {
    private String barcode;
    @Field("product_code")
    private String productCode;
    private String label;
    @Field("sale_price")
    private BigDecimal salePrice;
    @Field("cost_price")
    private BigDecimal costPrice;
    @Field("unit_of_measure")
    private UnitMeasure unitMeasure;
    @Field("conversion_factor")
    private BigDecimal conversionFactor;
}
