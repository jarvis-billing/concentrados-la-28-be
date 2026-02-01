package com.co.jarvis.entity;

import com.co.jarvis.enums.ESale;
import com.co.jarvis.enums.EVat;
import com.co.jarvis.util.mensajes.MessageConstants;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document(collection = "PRODUCTS")
public class Product {

    @Id
    private String id;
    private String description;
    @Field("sale_type")
    private ESale saleType;
    private String brand;
    @Field("product_code")
    private String productCode;
    @Field("category")
    private String category;
    private BigDecimal vatValue;
    private EVat vatType;
    private List<Presentation> presentations;
    private Stock stock;

    public boolean hasStock(BigDecimal amount) {
        return stock.getQuantity().compareTo(amount) >= 0;
    }

    // Reduce el stock
    public void reduceStock(BigDecimal amount) {
        if (!hasStock(amount)) {
            org.slf4j.LoggerFactory.getLogger(Product.class)
                    .warn(MessageConstants.STOCK_NOT_AVAILABLE + this.description);
        }
        stock.setQuantity(stock.getQuantity().subtract(amount));
    }

    // Aumenta el stock
    public void increaseStock(BigDecimal amount) {
        stock.setQuantity(stock.getQuantity().add(amount));
    }

    /**
     * Actualiza el costo unitario de una presentación específica.
     * @param barcode El código de barras que identifica la presentación.
     * @param newCost El nuevo valor de costPrice.
     * @return true si se encontró y actualizó, false de lo contrario.
     */
    public boolean updatePresentationCost(String barcode, BigDecimal newCost) {
        if (newCost == null || newCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo o nulo");
        }

        if (this.presentations == null || this.presentations.isEmpty()) {
            return false;
        }

        return this.presentations.stream()
                .filter(p -> p.getBarcode().equals(barcode))
                .findFirst()
                .map(p -> {
                    p.setCostPrice(newCost);
                    return true;
                })
                .orElse(false);
    }

}
