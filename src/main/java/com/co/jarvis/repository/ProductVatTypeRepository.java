package com.co.jarvis.repository;

import com.co.jarvis.entity.ProductVatType;
import com.co.jarvis.enums.EVat;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductVatTypeRepository extends MongoRepository<ProductVatType, String> {

    ProductVatType findByVatType(EVat eVat);
}
