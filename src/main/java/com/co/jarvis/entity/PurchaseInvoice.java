package com.co.jarvis.entity;

import com.co.jarvis.enums.EPurchaseInvoiceStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "PURCHASE_INVOICES")
public class PurchaseInvoice {

    @Id
    private String id;
    
    @Field("invoice_number")
    private String invoiceNumber;
    
    @Field("supplier_id")
    private String supplierId;
    
    @Field("supplier_name")
    private String supplierName;
    
    @Field("date")
    private OffsetDateTime date;
    
    @Field("items")
    private List<PurchaseInvoiceItem> items;
    
    @Field("total_amount")
    private BigDecimal totalAmount;
    
    @Field("status")
    private EPurchaseInvoiceStatus status;
    
    @Field("creation_user")
    private User creationUser;
    
    @Field("created_at")
    private OffsetDateTime createdAt;
    
    @Field("updated_at")
    private OffsetDateTime updatedAt;
}
