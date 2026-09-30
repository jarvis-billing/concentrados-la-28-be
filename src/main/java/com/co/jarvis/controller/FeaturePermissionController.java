package com.co.jarvis.controller;

import com.co.jarvis.dto.UserDto;
import com.co.jarvis.dto.featurepermission.CreatePermissionRequest;
import com.co.jarvis.dto.featurepermission.FeaturePermissionDto;
import com.co.jarvis.service.FeaturePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/api/feature-permissions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class FeaturePermissionController {

    private final FeaturePermissionService featurePermissionService;

    /** Lista todos los permisos de funcionalidades (ADMIN) */
    @GetMapping
    public ResponseEntity<List<FeaturePermissionDto>> findAll() {
        log.info("FeaturePermissionController -> findAll");
        return ResponseEntity.ok(featurePermissionService.findAll());
    }

    /**
     * Comprueba si un rol tiene acceso a una funcionalidad.
     * Disponible para cualquier usuario autenticado (usado por el guard del FE).
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> check(
            @RequestParam String featureKey,
            @RequestParam String role) {
        log.info("FeaturePermissionController -> check featureKey={} role={}", featureKey, role);
        boolean granted = featurePermissionService.isGranted(featureKey, role);
        return ResponseEntity.ok(Map.of("granted", granted));
    }

    /** Crea un nuevo permiso de funcionalidad (ADMIN) */
    @PostMapping
    public ResponseEntity<FeaturePermissionDto> create(
            @RequestBody CreatePermissionRequest request,
            Authentication auth) {
        log.info("FeaturePermissionController -> create featureKey={}", request.getFeatureKey());
        UserDto actor = (UserDto) auth.getPrincipal();
        return ResponseEntity.ok(featurePermissionService.create(request, actor.getFullName()));
    }

    /**
     * Verifica si una funcionalidad global está habilitada (sin importar el rol).
     * Retorna true si hay al menos un permiso activo y no expirado para la featureKey.
     */
    @GetMapping("/is-enabled")
    public ResponseEntity<Map<String, Boolean>> isEnabled(@RequestParam String featureKey) {
        log.info("FeaturePermissionController -> isEnabled featureKey={}", featureKey);
        boolean enabled = featurePermissionService.isEnabled(featureKey);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    /** Revoca un permiso (lo desactiva) — ADMIN */
    @DeleteMapping("/{id}")
    public ResponseEntity<FeaturePermissionDto> revoke(@PathVariable String id) {
        log.info("FeaturePermissionController -> revoke id={}", id);
        return ResponseEntity.ok(featurePermissionService.revoke(id));
    }
}
