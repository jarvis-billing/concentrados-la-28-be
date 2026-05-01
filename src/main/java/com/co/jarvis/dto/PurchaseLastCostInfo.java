package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Respuesta del endpoint GET /api/purchases/last-cost.
 * Devuelve el costo de la última compra registrada para una presentación dada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseLastCostInfo implements Serializable {

    private String presentationId;
    private String presentationBarcode;
    private String productDescription;
    private BigDecimal lastUnitCost;
    private BigDecimal lastVatRate;
    private BigDecimal lastVatPerUnit;
    private BigDecimal lastFreightPerUnit;
    private BigDecimal lastUnitTotalCost;
    private String lastInvoiceId;
    private String lastInvoiceNumber;
    private LocalDate lastInvoiceDate;
    private String lastSupplierId;
    private String lastSupplierName;
}
