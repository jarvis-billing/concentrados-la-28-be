package com.co.jarvis.dto;

import com.co.jarvis.enums.EAlertLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertDto implements Serializable {
    private String productId;
    private String productName;
    private String barcode;
    private Double currentStock;
    private Double minimumStock;
    private Double criticalStock;
    private String unitMeasure;
    private EAlertLevel alertLevel;
}
