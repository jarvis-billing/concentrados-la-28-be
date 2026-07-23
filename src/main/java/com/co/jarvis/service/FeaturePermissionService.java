package com.co.jarvis.service;

import com.co.jarvis.dto.featurepermission.CreatePermissionRequest;
import com.co.jarvis.dto.featurepermission.FeaturePermissionDto;

import java.util.List;

public interface FeaturePermissionService {

    /**
     * Comprueba si un rol tiene acceso a una funcionalidad.
     * Para permisos TEMPORARY solo es válido si no ha expirado.
     */
    boolean isGranted(String featureKey, String role);

    /** Lista todos los permisos (activos e inactivos) */
    List<FeaturePermissionDto> findAll();

    /** Crea un nuevo permiso */
    FeaturePermissionDto create(CreatePermissionRequest request, String grantedBy);

    /** Revoca (desactiva) un permiso por su ID */
    FeaturePermissionDto revoke(String id);
}
