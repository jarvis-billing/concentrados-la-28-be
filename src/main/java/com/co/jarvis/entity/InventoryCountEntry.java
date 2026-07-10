package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de conteo de una presentación dentro de una sesión de conteo físico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCountEntry {

    private String barcode;
    private String productId;
    private String description;
    private String presentationLabel;

    /** Stock registrado en el sistema al momento del conteo */
    private BigDecimal systemStock;

    /** Cantidad física contada por el operador */
    private BigDecimal countedQty;

    /** Diferencia = countedQty - systemStock */
    private BigDecimal difference;

    private LocalDateTime countedAt;
    private String countedBy;
}
