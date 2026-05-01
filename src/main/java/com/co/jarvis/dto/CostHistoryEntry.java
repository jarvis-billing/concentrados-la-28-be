package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entrada del historial de costos de compra para una presentación.
 * Respuesta del endpoint GET /api/purchases/cost-history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostHistoryEntry implements Serializable {

    private String invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private OffsetDateTime createdAt;
    private String supplierId;
    private String supplierName;
    private String presentationId;
    private String presentationBarcode;
    private String productDescription;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal freightAmount;
    private BigDecimal unitTotalCost;
}
