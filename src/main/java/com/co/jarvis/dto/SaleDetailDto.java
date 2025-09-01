package com.co.jarvis.dto;

import com.co.jarvis.dto.api.model.ProductApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SaleDetailDto {

    private String id;
    private ProductApi product;
    private BigDecimal amount;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private BigDecimal totalVat;
}
