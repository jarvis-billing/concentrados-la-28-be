package com.co.jarvis.entity;

import com.co.jarvis.enums.UnitMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Stock {
    private BigDecimal quantity;
    private UnitMeasure unitMeasure;
}
