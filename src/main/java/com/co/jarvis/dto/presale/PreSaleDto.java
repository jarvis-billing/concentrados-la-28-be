package com.co.jarvis.dto.presale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreSaleDto {

    private String id;
    private String preSaleNumber;
    private String status;
    private String sellerName;
    private String clientName;
    private List<PreSaleItemDto> items;
    private double totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime finalizedAt;
    private LocalDateTime billedAt;
    private String billingId;
    /** Número legible de la factura que facturó esta preventa (ej: FAC-00123) */
    private String billNumber;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private String billedBy;
}
