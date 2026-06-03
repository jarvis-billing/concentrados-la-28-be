package com.co.jarvis.repository;

import com.co.jarvis.entity.PreSale;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PreSaleRepository extends MongoRepository<PreSale, String> {

    Optional<PreSale> findByPreSaleNumber(String preSaleNumber);

    /** Retorna todas las preventas que tienen billingId asignado (fueron facturadas) */
    List<PreSale> findByBillingIdNotNull();
}
