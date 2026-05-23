package com.co.jarvis.dto.presale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePreSaleRequest {

    private String sellerName;
    private List<PreSaleItemDto> items;
    private double totalAmount;
    private String notes;
}
