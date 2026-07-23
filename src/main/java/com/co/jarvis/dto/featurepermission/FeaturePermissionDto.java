package com.co.jarvis.dto.featurepermission;

import com.co.jarvis.enums.PermissionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FeaturePermissionDto {
    private String id;
    private String featureKey;
    private String featureName;
    private List<String> grantedRoles;
    private PermissionType type;
    private LocalDateTime expiresAt;
    private String grantedBy;
    private LocalDateTime grantedAt;
    private boolean active;
    private String notes;
    /** Indica si el permiso temporal ya expiró (aunque active=true) */
    private boolean expired;
}
