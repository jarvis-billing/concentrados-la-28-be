package com.co.jarvis.entity;

import com.co.jarvis.enums.BatchStatus;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "BATCHES")
public class Batch {

    @Id
    private String id;

    @Field("batch_number")
    private Integer batchNumber;

    @Field("product_id")
    private String productId;

    @Field("entry_date")
    private LocalDate entryDate;

    @Field("sale_price")
    private BigDecimal salePrice;

    @Field("initial_stock")
    private Integer initialStock;

    @Field("current_stock")
    private Integer currentStock;

    @Field("unit_measure")
    private String unitMeasure;

    @Field("price_validity_days")
    private Integer priceValidityDays;

    @Field("expiration_date")
    private LocalDate expirationDate;

    @Field("status")
    private BatchStatus status;

    @Field("purchase_invoice_id")
    private String purchaseInvoiceId;

    @Field("notes")
    private String notes;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
