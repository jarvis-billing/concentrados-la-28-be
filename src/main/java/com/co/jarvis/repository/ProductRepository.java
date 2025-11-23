package com.co.jarvis.repository;

import com.co.jarvis.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {

    @Query("{ 'description' : { $regex: ?0, $options: 'i' } }")
    List<Product> findByDescriptionContainingIgnoreCase(String description);

    Page<Product> findAll(Pageable pageable);

    @Query("{ $or: [ { 'presentations.barcode': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }")
    Page<Product> findByPresentationsBarcodeOrDescriptionContainingIgnoreCase(String barcode, String description, Pageable pageable);

    Product findByPresentationsBarcode(String barcode);

    @Aggregation(pipeline = {
            "{ $unwind: '$presentations' }",
            "{ $match: { 'presentations.barcode': { $regex: '^[0-9]{4}$' } } }", // Solo barcodes numéricos de 4 dígitos para códigos internos
            "{ $addFields: { 'presentations.barcodeAsNumber': { $toInt: '$presentations.barcode' } } }",
            "{ $sort: { 'presentations.barcodeAsNumber': -1 } }",
            "{ $limit: 1 }",
            "{ $project: { 'barcode': '$presentations.barcode' } }"
    })
    String findHighestBarcodeAsString();

    @Aggregation(pipeline = {
            "{ $sort: { 'productCode': -1 } }",
            "{ $limit: 1 }"
    })
    Product findTopByOrderByProductCodeDesc();
}
