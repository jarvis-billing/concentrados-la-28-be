package com.co.jarvis.entity;

import com.co.jarvis.enums.EAuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entrada de auditoría embebida para registrar operaciones sobre una entidad.
 * Se almacena como subdocumento en MongoDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntry {

    private String userId;          // numberIdentity del usuario
    private String userName;        // Nombre completo del usuario
    private EAuditAction action;    // Tipo de operación realizada
    private LocalDateTime timestamp;
    private String details;         // Notas opcionales sobre la operación

    /**
     * Referencia al sub-elemento afectado dentro de la entidad (opcional).
     * Ejemplos: "presentation:7702001123456" para identificar una presentación por barcode.
     */
    private String entityRef;

    /**
     * Nombre del campo modificado (opcional). Ejemplos: "salePrice", "costPrice", "stock".
     */
    private String fieldName;

    /**
     * Valor anterior del campo, serializado como texto (opcional).
     */
    private String oldValue;

    /**
     * Valor nuevo del campo, serializado como texto (opcional).
     */
    private String newValue;
}
