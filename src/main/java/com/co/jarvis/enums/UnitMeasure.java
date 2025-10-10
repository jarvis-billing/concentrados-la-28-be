package com.co.jarvis.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum UnitMeasure {
    KILOGRAMOS("Kg", new BigDecimal("1.0")),
    UNIDAD("Unit", BigDecimal.ONE), // No requiere conversión
    CENTIMETROS("Cm", new BigDecimal("0.01")), // 1 cm = 0.01 m
    METROS("M", BigDecimal.ONE), // 1 m = 1 m
    LITROS("L", BigDecimal.ONE), // 1 L = 1 L
    MILILITROS("Ml", new BigDecimal("0.001")); // 1 ml = 0.001 L

    private final String sigma;
    private final BigDecimal conversionFactor;

    UnitMeasure(String description, BigDecimal conversionFactor) {
        this.sigma = description;
        this.conversionFactor = conversionFactor;
    }

}
