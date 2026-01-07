package com.co.jarvis.entity;

import com.co.jarvis.enums.EAdjustmentReason;
import com.co.jarvis.enums.EAdjustmentType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "INVENTORY_ADJUSTMENTS")
public class InventoryAdjustment {

    @Id
    private String id;

    @Field("date")
    @Indexed
    private LocalDateTime date;

    @Field("product_id")
    @Indexed
    private String productId;

    @Field("product")
    private ProductReference product;

    @Field("presentation_barcode")
    private String presentationBarcode;

    @Field("adjustment_type")
    private EAdjustmentType adjustmentType;

    @Field("quantity")
    private Double quantity;

    @Field("previous_stock")
    private Double previousStock;

    @Field("new_stock")
    private Double newStock;

    @Field("reason")
    private EAdjustmentReason reason;

    @Field("notes")
    private String notes;

    @Field("user_id")
    private String userId;

    @Field("user")
    private UserReference user;

    @Field("evidence_url")
    private String evidenceUrl;

    @Field("requires_authorization")
    private Boolean requiresAuthorization;

    @Field("authorized_by")
    private String authorizedBy;

    @Field("authorized_at")
    private LocalDateTime authorizedAt;

    @Field("created_at")
    private LocalDateTime createdAt;
}
