package com.co.jarvis.entity;

import com.co.jarvis.enums.EReturnResolution;
import com.co.jarvis.enums.EReturnStatus;
import com.co.jarvis.enums.EReturnType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "MERCHANDISE_RETURNS")
public class MerchandiseReturn {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("return_number")
    private String returnNumber;

    @Field("return_type")
    private EReturnType returnType;

    @Field("original_document_id")
    private String originalDocumentId;

    @Field("original_document_number")
    private String originalDocumentNumber;

    @Field("return_date")
    private OffsetDateTime returnDate;

    @Field("items")
    private List<MerchandiseReturnItem> items;

    @Field("status")
    private EReturnStatus status;

    @Field("resolution")
    private EReturnResolution resolution;

    // Referencia al cliente (para devoluciones de venta)
    @Field("client_id")
    private String clientId;

    @Field("client_name")
    private String clientName;

    // Referencia al proveedor (para devoluciones de compra)
    @Field("supplier_id")
    private String supplierId;

    @Field("supplier_name")
    private String supplierName;

    // Detalles financieros del reembolso
    @Field("refund_method")
    private String refundMethod;

    @Field("bank_account_id")
    private String bankAccountId;

    @Field("bank_account_name")
    private String bankAccountName;

    // Monto de saldo a favor restaurado (cuando la venta original fue pagada parcial o totalmente con crédito)
    @Field("credit_restored")
    private BigDecimal creditRestored;

    // Monto real a reembolsar en efectivo/transferencia (totalAmount - creditRestored)
    @Field("cash_refund_amount")
    private BigDecimal cashRefundAmount;

    // Totales
    @Field("subtotal")
    private BigDecimal subtotal;

    @Field("total_vat")
    private BigDecimal totalVat;

    @Field("total_amount")
    private BigDecimal totalAmount;

    // Metadatos
    @Field("notes")
    private String notes;

    @Field("cancel_reason")
    private String cancelReason;

    @Field("created_by")
    private UserReference createdBy;

    @Field("created_at")
    private OffsetDateTime createdAt;

    @Field("processed_at")
    private OffsetDateTime processedAt;

    @Field("cancelled_at")
    private OffsetDateTime cancelledAt;
}
