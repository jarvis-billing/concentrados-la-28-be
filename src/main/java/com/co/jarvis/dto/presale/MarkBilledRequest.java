package com.co.jarvis.dto.presale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkBilledRequest {

    private String billingId;
    /** Número legible de la factura (ej: FAC-00123) para mostrarlo en la preventa */
    private String billNumber;
}
