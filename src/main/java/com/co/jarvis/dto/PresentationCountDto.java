package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationCountDto {
    
    private String presentationBarcode;
    
    private String presentationLabel;
    
    private BigDecimal quantity;
    
    private BigDecimal fixedAmount;
    
    private Boolean isBulk;
    
    private BigDecimal calculatedStock;
}
