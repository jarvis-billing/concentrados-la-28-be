package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductStockTraceDto {

    private List<PurchaseTrace> lastPurchases;
    private List<InventoryCountTrace> lastInventoryCounts;
    private List<SaleTrace> lastSales;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PurchaseTrace {
        private String purchaseDate;
        private String registrationDate;
        private String supplier;
        private BigDecimal quantity;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InventoryCountTrace {
        private String date;
        private Double physicalStock;
        private Double systemStock;
        private Double difference;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaleTrace {
        private String saleDate;
        private String invoiceNumber;
        private BigDecimal quantity;
    }
}
