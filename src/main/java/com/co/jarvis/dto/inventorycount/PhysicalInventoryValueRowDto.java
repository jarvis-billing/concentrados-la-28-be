package com.co.jarvis.dto.inventorycount;

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
public class PhysicalInventoryValueRowDto implements Serializable {
    private String productId;
    private String physicalInventoryId;
    private String description;
    private String presentationLabel;
    private String barcode;
    private String countDate;
    private Double physicalStock;
    private String unitMeasure;
    private BigDecimal fixedAmount;
    private BigDecimal unitCost;
    private BigDecimal salePrice;
    private BigDecimal totalValue;
}
