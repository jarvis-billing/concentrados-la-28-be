package com.co.jarvis.dto;

import com.co.jarvis.enums.EPaymentType;
import com.co.jarvis.enums.EPurchaseInvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseInvoiceDto implements Serializable {
    private String id;
    private String invoiceNumber;
    private SupplierRefDto supplier;
    private LocalDate invoiceDate;
    private EPaymentType paymentType;
    private OffsetDateTime date;
    private List<PurchaseInvoiceItemDto> items;
    private BigDecimal totalAmount;
    private EPurchaseInvoiceStatus status;
    private UserDto creationUser;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
