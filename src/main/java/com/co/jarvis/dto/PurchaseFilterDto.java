package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseFilterDto implements Serializable {
    /** Filtro legacy por campo 'date' (fecha de factura) */
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;

    /** Filtro por fecha de ingreso al sistema (created_at) */
    private LocalDate createdAtFrom;
    private LocalDate createdAtTo;

    private SupplierRefDto supplier;
    private String invoiceNumber;

    /** Filtro por código de barras de presentación en los ítems */
    private String productBarcode;
}
