package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesTotalsResponse implements Serializable {

    private long totalInvoices;
    private BigDecimal totalSubtotal;
    private BigDecimal totalIva;
    private BigDecimal totalGeneral;

    private long countContado;
    private long countCredito;
    private BigDecimal totalContado;
    private BigDecimal totalCredito;

    private List<PaymentMethodTotalDto> paymentMethodTotals;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentMethodTotalDto implements Serializable {
        private String method;
        private long count;
        private BigDecimal total;
    }
}
