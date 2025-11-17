package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntryDto implements Serializable {
    private String method;      // e.g., "EFECTIVO", "TRANSFERENCIA"
    private BigDecimal amount;  // decimal(18,2)
    private String reference;   // optional
}
