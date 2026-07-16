package com.co.jarvis.dto.inventorycount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UncountedProductDto {

    private String barcode;
    private String productId;
    private String presentationId;
    private String description;
    private String presentationLabel;
    private BigDecimal systemStock;
    /** false = presentación marcada como inactiva/oculta */
    private Boolean active;
}
