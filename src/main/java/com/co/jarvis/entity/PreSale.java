package com.co.jarvis.entity;

import com.co.jarvis.enums.PreSaleStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "PRE_SALES")
@CompoundIndex(name = "status_createdAt_idx", def = "{'status': 1, 'createdAt': -1}")
public class PreSale {

    @Id
    private String id;

    @Indexed(unique = true)
    private String preSaleNumber;

    private PreSaleStatus status;
    private String sellerName;
    private List<PreSaleItem> items;
    private double totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime finalizedAt;
    private LocalDateTime billedAt;
    private String billingId;
    /** Número de factura legible (ej: FAC-00123) que facturó esta preventa */
    private String billNumber;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private String billedBy;
}
