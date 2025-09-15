package com.co.jarvis.repository;

import com.co.jarvis.entity.Catalog;
import com.co.jarvis.enums.CatalogType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CatalogRepository extends MongoRepository<Catalog, String> {

    List<Catalog> findByType(CatalogType type);
}
