package com.co.jarvis.dto.inventorycount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhysicalInventoryValueReportDto implements Serializable {
    private String fromDate;
    private String toDate;
    private String generatedAt;
    private BigDecimal grandTotal;
    private List<PhysicalInventoryValueRowDto> rows;
    private List<PhysicalInventoryValueRowDto> noCostRows;
    private List<PhysicalInventoryValueRowDto> noCostZeroRows;
    private List<PhysicalInventoryValueRowDto> nullBarcodeRows;
    private List<PhysicalInventoryUncountedDto> uncountedProducts;
}
