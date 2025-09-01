package com.co.jarvis.entity;

import com.co.jarvis.enums.EStatusOrder;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document(collection = "ORDERS")
public class Order {

    @Id
    private String id;
    private Long orderNumber;
    private List<Product> products;
    private BigDecimal totalOrder;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private User creationUser;
    private EStatusOrder status;
    private Client client;
}
