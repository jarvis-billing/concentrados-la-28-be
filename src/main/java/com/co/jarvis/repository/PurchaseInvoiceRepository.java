package com.co.jarvis.repository;

import com.co.jarvis.entity.PurchaseInvoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface PurchaseInvoiceRepository extends MongoRepository<PurchaseInvoice, String> {
    
    @Query("{ 'invoiceNumber': ?0 }")
    PurchaseInvoice findByInvoiceNumber(String invoiceNumber);
    
    @Query("{ 'supplierId': ?0 }")
    List<PurchaseInvoice> findBySupplierId(String supplierId);
    
    @Query("{ 'date': { $gte: ?0, $lte: ?1 } }")
    List<PurchaseInvoice> findByDateBetween(OffsetDateTime dateFrom, OffsetDateTime dateTo);
}
