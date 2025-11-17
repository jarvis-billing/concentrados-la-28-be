package com.co.jarvis.repository;

import com.co.jarvis.entity.Supplier;
import com.co.jarvis.enums.DocumentType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends MongoRepository<Supplier, String> {
    Optional<Supplier> findByDocumentTypeAndIdNumber(DocumentType documentType, String idNumber);
}
