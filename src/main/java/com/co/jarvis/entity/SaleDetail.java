package com.co.jarvis.entity;

import com.co.jarvis.dto.api.model.ProductApi;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "SALE_DETAIL")
public class SaleDetail {

    @Id
    private String id;
    private ProductApi product;
    private BigDecimal amount;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private BigDecimal totalVat;
}
