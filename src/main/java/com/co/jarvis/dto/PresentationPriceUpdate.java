package com.co.jarvis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Single presentation price update targeting a product by id and its presentation by barcode.
 * Both {@code salePrice} and {@code costPrice} are optional and independent:
 * whichever is null will not be modified.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PresentationPriceUpdate implements Serializable {

    @NotBlank(message = "productId es obligatorio")
    private String productId;

    @NotBlank(message = "barcode es obligatorio")
    private String barcode;

    @PositiveOrZero(message = "salePrice no puede ser negativo")
    private BigDecimal salePrice;

    @PositiveOrZero(message = "costPrice no puede ser negativo")
    private BigDecimal costPrice;
}
