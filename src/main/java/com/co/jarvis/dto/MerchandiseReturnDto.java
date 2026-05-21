package com.co.jarvis.dto;

import com.co.jarvis.enums.EReturnResolution;
import com.co.jarvis.enums.EReturnStatus;
import com.co.jarvis.enums.EReturnType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchandiseReturnDto {
    private String id;
    private String returnNumber;
    private EReturnType returnType;
    private String originalDocumentId;
    private String originalDocumentNumber;
    private OffsetDateTime returnDate;
    private List<MerchandiseReturnItemDto> items;
    private EReturnStatus status;
    private EReturnResolution resolution;

    // Cliente (para devoluciones de venta)
    private String clientId;
    private String clientName;

    // Proveedor (para devoluciones de compra)
    private String supplierId;
    private String supplierName;

    // Detalles financieros
    private String refundMethod;
    private String bankAccountId;
    private String bankAccountName;

    // Campos de desglose del reembolso (calculados por el backend)
    private BigDecimal creditRestored;    // Porción que vuelve como saldo a favor
    private BigDecimal cashRefundAmount;  // Porción que se entrega en efectivo/transferencia

    // Totales
    private BigDecimal subtotal;
    private BigDecimal totalVat;
    private BigDecimal totalAmount;

    // Metadatos
    private String notes;
    private String cancelReason;
    private UserDto createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime processedAt;
    private OffsetDateTime cancelledAt;
}
