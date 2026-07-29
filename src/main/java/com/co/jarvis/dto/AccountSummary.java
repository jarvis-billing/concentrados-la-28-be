package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummary {

    private String clientId;
    private String clientName;
    private String clientIdNumber;
    private String clientPhone;
    private String clientAddress;
    private String clientNickname;
    private BigDecimal totalDebt;
    private BigDecimal totalPaid;
    private BigDecimal currentBalance;
    private LocalDateTime lastPaymentDate;
    private Long daysSinceLastPayment;

    /** Historial de pagos con saldo acumulado antes y después de cada abono */
    private List<PaymentWithBalance> payments;

    /** Facturas a crédito del cliente — detalle completo para el reporte */
    private List<BillingDto> creditBillings;

    /** Líneas planas para el PDF: cabeceras de factura + productos, precalculadas */
    private List<BillingDetailLine> billingDetailLines;

    /** Transacciones manuales: deudas del cuaderno, ajustes, devoluciones */
    private List<ManualTransaction> manualTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualTransaction {
        private String id;
        private String type;          // MANUAL_DEBT, ADJUSTMENT, RETURN_ADJUSTMENT
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String notes;
        private String source;        // MIGRACION_CUADERNO, etc.
        private LocalDate transactionDate;
        private String createdBy;
        private LocalDateTime createdAt;
    }

    /**
     * Fila plana para el sub-dataset de facturas en el PDF.
     * rowType = "HEADER" → datos de la factura; rowType = "PRODUCT" → línea de producto.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingDetailLine {
        private String rowType;              // "HEADER" | "PRODUCT"
        private String billNumber;
        private String billingDate;          // pre-formateado "dd/MM/yyyy"
        private BigDecimal billTotal;
        private String productDescription;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentWithBalance {
        private String id;
        private BigDecimal amount;
        private String paymentMethod;
        private String bankAccountName;      // cuenta destino para transferencias
        private String reference;
        private String notes;
        private LocalDateTime paymentDate;
        private String createdBy;
        /** Saldo pendiente ANTES de aplicar este pago */
        private BigDecimal balanceBefore;
        /** Saldo pendiente DESPUÉS de aplicar este pago */
        private BigDecimal balanceAfter;
    }
}
