package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO simplificado para referencias de proveedor (solo id y name)
 * Usado en PurchaseInvoiceDto para mantener consistencia con el frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRefDto implements Serializable {
    private String id;
    private String name;
}
