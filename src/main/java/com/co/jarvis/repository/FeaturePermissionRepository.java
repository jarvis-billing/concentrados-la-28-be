package com.co.jarvis.repository;

import com.co.jarvis.entity.FeaturePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeaturePermissionRepository extends MongoRepository<FeaturePermission, String> {

    List<FeaturePermission> findByFeatureKeyAndActiveTrue(String featureKey);

    List<FeaturePermission> findByActiveTrue();
}
