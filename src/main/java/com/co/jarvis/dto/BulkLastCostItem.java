package com.co.jarvis.dto;

import java.io.Serializable;

public record BulkLastCostItem(
    String barcode,
    String presentationId,
    String productDescription,
    double lastUnitCost,
    double lastVatRate,
    double lastVatPerUnit,
    double lastFreightPerUnit,
    double lastUnitTotalCost,
    String lastInvoiceId,
    String lastInvoiceNumber,
    String lastInvoiceDate,
    String lastSupplierId,
    String lastSupplierName,
    CostSource source
) implements Serializable {}
