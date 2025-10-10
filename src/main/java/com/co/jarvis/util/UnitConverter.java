package com.co.jarvis.util;

import com.co.jarvis.enums.UnitMeasure;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilidad para conversión de unidades de medida a unidad base
 */
@Slf4j
public class UnitConverter {
    
    private static final BigDecimal EPSILON = new BigDecimal("0.000001");
    
    /**
     * Convierte un valor de una unidad a la unidad base especificada
     * 
     * @param value Valor a convertir
     * @param fromUnit Unidad origen
     * @param baseUnit Unidad base destino (ej: "kg", "cm")
     * @return Valor convertido a la unidad base
     */
    public static BigDecimal toBase(BigDecimal value, UnitMeasure fromUnit, String baseUnit) {
        if (value == null || fromUnit == null || baseUnit == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Conversiones para WEIGHT (base: kg)
            if ("kg".equalsIgnoreCase(baseUnit)) {
                return switch (fromUnit) {
                    case KILOGRAMOS -> value; // Ya está en kg
                    default -> {
                        log.warn("Unidad {} no compatible con base kg", fromUnit);
                        yield value;
                    }
                };
            }
            
            // Conversiones para LONGITUDE (base: cm)
            if ("cm".equalsIgnoreCase(baseUnit)) {
                return switch (fromUnit) {
                    case CENTIMETROS -> value; // Ya está en cm
                    case METROS -> value.multiply(new BigDecimal("100")); // 1 m = 100 cm
                    default -> {
                        log.warn("Unidad {} no compatible con base cm", fromUnit);
                        yield value;
                    }
                };
            }
            
            // Conversiones para VOLUME (base: L)
            if ("L".equalsIgnoreCase(baseUnit) || "l".equalsIgnoreCase(baseUnit)) {
                return switch (fromUnit) {
                    case LITROS -> value; // Ya está en L
                    case MILILITROS -> value.divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP); // 1000 ml = 1 L
                    default -> {
                        log.warn("Unidad {} no compatible con base L", fromUnit);
                        yield value;
                    }
                };
            }
            
            // Si no hay conversión específica, retornar el valor original
            return value;
            
        } catch (Exception e) {
            log.error("Error convirtiendo {} {} a {}: {}", value, fromUnit, baseUnit, e.getMessage());
            return value;
        }
    }
    
    /**
     * Redondea un valor a un número específico de decimales
     * 
     * @param value Valor a redondear
     * @param decimals Número de decimales
     * @return Valor redondeado
     */
    public static BigDecimal round(BigDecimal value, int decimals) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(decimals, RoundingMode.HALF_UP);
    }
    
    /**
     * Verifica si un valor es efectivamente cero (considerando epsilon)
     * 
     * @param value Valor a verificar
     * @return true si el valor es cero o muy cercano a cero
     */
    public static boolean isEffectivelyZero(BigDecimal value) {
        if (value == null) {
            return true;
        }
        return value.abs().compareTo(EPSILON) < 0;
    }
}
