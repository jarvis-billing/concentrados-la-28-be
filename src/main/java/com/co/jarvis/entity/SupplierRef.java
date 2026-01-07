package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase embebida para referencias de proveedor en documentos MongoDB
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRef {
    private String id;
    private String name;
}
