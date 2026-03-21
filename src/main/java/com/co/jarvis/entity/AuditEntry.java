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
}
