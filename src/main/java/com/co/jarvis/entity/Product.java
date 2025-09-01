package com.co.jarvis.entity;

import com.co.jarvis.enums.EVat;
import com.co.jarvis.enums.ESale;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

import static com.co.jarvis.enums.ESale.UNIT;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document(collection = "PRODUCTS")
public class Product {

    @Id
    private String id;
    private String description;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal totalValue;
    private ESale saleType;
    private BigDecimal stockKg;
    private BigDecimal stockUnit;
    private String brand;
    private String productCode;
    private String category;
    private BigDecimal vatValue;
    private EVat vatType;
    private BigDecimal cost;
    private List<Presentation> presentations;

    public boolean hasStock(BigDecimal amount) {
        if (saleType == UNIT) {
            return stockUnit.compareTo(amount) >= 0;
        }
        return stockKg.compareTo(amount) >= 0;
    }

    // Reduce el stock
    public void reduceStock(BigDecimal amount) {
        if (!hasStock(amount)) {
            throw new IllegalArgumentException("No hay stock suficiente");
        }
        if (saleType == UNIT) {
            stockUnit = stockUnit.subtract(amount);
            return;
        }
        stockKg = stockKg.subtract(amount);
    }

    // Aumenta el stock
    public void increaseStock(BigDecimal amount) {
        if (saleType == UNIT) {
            stockUnit = stockUnit.add(amount);
            return;
        }
        stockKg = stockKg.add(amount);
    }

}
