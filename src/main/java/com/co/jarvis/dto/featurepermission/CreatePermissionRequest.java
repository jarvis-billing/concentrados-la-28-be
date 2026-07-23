package com.co.jarvis.dto.featurepermission;

import com.co.jarvis.enums.PermissionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreatePermissionRequest {
    private String featureKey;
    private String featureName;
    private List<String> grantedRoles;
    private PermissionType type;
    /** Requerido si type = TEMPORARY */
    private LocalDateTime expiresAt;
    private String notes;
}
