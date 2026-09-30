package com.co.jarvis.dto.inventorycount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhysicalInventoryUncountedDto implements Serializable {
    private String description;
    private String label;
    private String barcode;
    private String unitMeasure;
}
