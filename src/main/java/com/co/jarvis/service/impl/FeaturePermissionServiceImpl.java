package com.co.jarvis.service.impl;

import com.co.jarvis.dto.featurepermission.CreatePermissionRequest;
import com.co.jarvis.dto.featurepermission.FeaturePermissionDto;
import com.co.jarvis.entity.FeaturePermission;
import com.co.jarvis.enums.PermissionType;
import com.co.jarvis.repository.FeaturePermissionRepository;
import com.co.jarvis.service.FeaturePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeaturePermissionServiceImpl implements FeaturePermissionService {

    private final FeaturePermissionRepository repository;

    @Override
    public boolean isGranted(String featureKey, String role) {
        List<FeaturePermission> permissions = repository.findByFeatureKeyAndActiveTrue(featureKey);
        LocalDateTime now = LocalDateTime.now();

        return permissions.stream().anyMatch(p -> {
            if (p.getGrantedRoles() == null || !p.getGrantedRoles().contains(role)) return false;
            if (p.getType() == PermissionType.PERMANENT) return true;
            // TEMPORARY: válido solo si expiresAt es futuro
            return p.getExpiresAt() != null && p.getExpiresAt().isAfter(now);
        });
    }

    @Override
    public List<FeaturePermissionDto> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public FeaturePermissionDto create(CreatePermissionRequest request, String grantedBy) {
        FeaturePermission permission = FeaturePermission.builder()
                .featureKey(request.getFeatureKey())
                .featureName(request.getFeatureName())
                .grantedRoles(request.getGrantedRoles())
                .type(request.getType())
                .expiresAt(request.getExpiresAt())
                .grantedBy(grantedBy)
                .grantedAt(LocalDateTime.now())
                .active(true)
                .notes(request.getNotes())
                .build();

        FeaturePermission saved = repository.save(permission);
        log.info("FeaturePermission creado: featureKey={} roles={} type={} by={}",
                saved.getFeatureKey(), saved.getGrantedRoles(), saved.getType(), grantedBy);
        return toDto(saved);
    }

    @Override
    public FeaturePermissionDto revoke(String id) {
        FeaturePermission permission = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado: " + id));
        permission.setActive(false);
        FeaturePermission saved = repository.save(permission);
        log.info("FeaturePermission revocado: id={} featureKey={}", id, saved.getFeatureKey());
        return toDto(saved);
    }

    public FeaturePermissionDto toDto(FeaturePermission p) {
        LocalDateTime now = LocalDateTime.now();
        boolean expired = p.getType() == PermissionType.TEMPORARY
                && (p.getExpiresAt() == null || !p.getExpiresAt().isAfter(now));
        return FeaturePermissionDto.builder()
                .id(p.getId())
                .featureKey(p.getFeatureKey())
                .featureName(p.getFeatureName())
                .grantedRoles(p.getGrantedRoles())
                .type(p.getType())
                .expiresAt(p.getExpiresAt())
                .grantedBy(p.getGrantedBy())
                .grantedAt(p.getGrantedAt())
                .active(p.isActive())
                .notes(p.getNotes())
                .expired(expired)
                .build();
    }
}
