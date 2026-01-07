package com.co.jarvis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Documento embebido para referencias de usuario en movimientos de inventario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReference {
    private String id;
    private String fullName;
}
