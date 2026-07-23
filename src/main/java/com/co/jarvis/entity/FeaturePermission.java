package com.co.jarvis.entity;

import com.co.jarvis.enums.PermissionType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Permiso de acceso a una funcionalidad específica de la aplicación.
 * Permite asignar roles de usuario a funcionalidades protegidas,
 * ya sea de forma permanente o con una fecha de expiración.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "FEATURE_PERMISSIONS")
public class FeaturePermission {

    @Id
    private String id;

    /** Clave de la funcionalidad, ej. "INVENTORY_COUNT" */
    @Indexed
    private String featureKey;

    /** Nombre descriptivo de la funcionalidad */
    private String featureName;

    /** Roles que reciben el permiso, ej. ["VENDEDOR"] */
    private List<String> grantedRoles;

    /** PERMANENT = sin expiración; TEMPORARY = válido hasta expiresAt */
    private PermissionType type;

    /** Solo aplica cuando type = TEMPORARY */
    private LocalDateTime expiresAt;

    private String grantedBy;

    @Builder.Default
    private LocalDateTime grantedAt = LocalDateTime.now();

    @Builder.Default
    private boolean active = true;

    private String notes;
}
