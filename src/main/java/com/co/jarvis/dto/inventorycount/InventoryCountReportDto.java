package com.co.jarvis.dto.inventorycount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCountReportDto {

    private InventoryCountSessionDto session;

    /** Presentaciones que fueron contadas en esta sesión */
    private List<InventoryCountEntryDto> counted;

    /** Presentaciones que NO fueron contadas */
    private List<UncountedProductDto> uncounted;

    private int totalPresentations;
    private int totalCounted;
    private int totalUncounted;
    private double coveragePercent;
}
