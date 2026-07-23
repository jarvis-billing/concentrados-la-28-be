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
public class BulkCountRequest {
    /** Entradas de conteo para múltiples presentaciones de un mismo producto. */
    private List<RecordCountRequest> entries;
}
