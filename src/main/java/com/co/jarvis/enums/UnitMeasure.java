package com.co.jarvis.enums;

import java.math.BigDecimal;

public enum UnitMeasure {
    KG("Kilogramos", new BigDecimal("1.0")),
    UNIT("Unidad", BigDecimal.ONE); // No requiere conversión

    private final String description;
    private final BigDecimal conversionFactor;

    UnitMeasure(String description, BigDecimal conversionFactor) {
        this.description = description;
        this.conversionFactor = conversionFactor;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getConversionFactor() {
        return conversionFactor;
    }
}
