package com.co.jarvis.repository;

import com.co.jarvis.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {

    Product findByBarcode(String barcode);

    @Query("{ 'description' : { $regex: ?0, $options: 'i' } }")
    List<Product> findByDescriptionContainingIgnoreCase(String description);

    Page<Product> findAll(Pageable pageable);

    @Query("{ $or: [ { 'barcode': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }")
    Page<Product> findByBarcodeOrDescriptionContainingIgnoreCase(String barcode, String description, Pageable pageable);

}
