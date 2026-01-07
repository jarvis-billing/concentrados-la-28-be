package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDashboardDto implements Serializable {
    private Integer totalProducts;
    private BigDecimal totalInventoryValue;
    private Integer lowStockProducts;
    private Integer criticalStockProducts;
    private Integer outOfStockProducts;
    private List<ProductSummaryDto> topSellingProducts;
    private List<ProductSummaryDto> lowRotationProducts;
    private List<StockAlertDto> stockAlerts;
}
