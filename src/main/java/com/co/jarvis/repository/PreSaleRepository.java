package com.co.jarvis.repository;

import com.co.jarvis.entity.PreSale;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PreSaleRepository extends MongoRepository<PreSale, String> {

    Optional<PreSale> findByPreSaleNumber(String preSaleNumber);
}
