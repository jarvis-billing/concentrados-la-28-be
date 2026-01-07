package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Documento embebido para referencias de producto en movimientos de inventario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReference {
    private String id;
    private String description;
    private String barcode;
}
