package com.co.jarvis.dto.inventorycount;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhysicalInventoryValueReportFilter {
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
