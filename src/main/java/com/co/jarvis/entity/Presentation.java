package com.co.jarvis.entity;

import com.co.jarvis.enums.UnitMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Presentation {

    /**
     * Identificador único de la presentación dentro del producto.
     * Se genera automáticamente como UUID si viene nulo.
     * Permite actualizar cualquier campo (incluyendo barcode) sin depender de él.
     */
    @Id
    private String id;

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
    @Field("is_bulk")
    private Boolean isBulk;
    @Field("is_fixed_amount")
    private Boolean isFixedAmount;
    @Field("fixed_amount")
    private BigDecimal fixedAmount;

}
