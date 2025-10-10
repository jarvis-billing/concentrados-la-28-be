package com.co.jarvis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO para representar el stock en formato legible (bultos/rollos + sobrante)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DisplayStock implements Serializable {
    
    /**
     * Tipo de stock: WEIGHT (bultos) o LONGITUDE (rollos), null si no aplica
     */
    private String kind;
    
    /**
     * Tamaño del paquete/bulto/rollo en unidad base (kg o cm)
     */
    private BigDecimal packSize;
    
    /**
     * Cantidad de paquetes completos
     */
    private Integer packs;
    
    /**
     * Cantidad sobrante en unidad base
     */
    private BigDecimal remainder;
    
    /**
     * Unidad base: 'kg', 'cm', etc.
     */
    private String unit;
    
    /**
     * Etiqueta legible: "12 bultos + 8 kg" o "248 kg"
     */
    private String label;
    
    /**
     * Timestamp de cálculo (ISO 8601)
     */
    private String computedAt;
}
