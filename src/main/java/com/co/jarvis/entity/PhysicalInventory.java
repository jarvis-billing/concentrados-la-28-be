package com.co.jarvis.entity;

import com.co.jarvis.enums.EAdjustmentReason;
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
@Document(collection = "PHYSICAL_INVENTORIES")
public class PhysicalInventory {

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

    @Field("system_stock")
    private Double systemStock;

    @Field("physical_stock")
    private Double physicalStock;

    @Field("difference")
    private Double difference;

    @Field("adjustment_reason")
    private EAdjustmentReason adjustmentReason;

    @Field("notes")
    private String notes;

    @Field("user_id")
    private String userId;

    @Field("user")
    private UserReference user;

    @Field("created_at")
    private LocalDateTime createdAt;
}
