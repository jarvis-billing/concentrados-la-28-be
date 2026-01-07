package com.co.jarvis.entity;

import com.co.jarvis.enums.EMovementType;
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
@Document(collection = "INVENTORY_MOVEMENTS")
public class InventoryMovement {

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

    @Field("movement_type")
    @Indexed
    private EMovementType movementType;

    @Field("quantity")
    private Double quantity;

    @Field("previous_stock")
    private Double previousStock;

    @Field("new_stock")
    private Double newStock;

    @Field("unit_measure")
    private String unitMeasure;

    @Field("reference")
    private String reference;

    @Field("user_id")
    private String userId;

    @Field("user")
    private UserReference user;

    @Field("notes")
    private String notes;

    @Field("created_at")
    private LocalDateTime createdAt;
}
