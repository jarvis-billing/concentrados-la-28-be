package com.co.jarvis.dto.presale;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreSaleItemDto {

    private String barcode;
    private String productId;
    private String description;
    private String saleType;
    private String unitMeasure;
    private String presentationLabel;
    private double price;
    private double amount;

    @JsonProperty("isBulk")
    private boolean isBulk;

    private Double bulkInputAmount;
    private double subTotal;
}
