package com.co.jarvis.repository;

import com.co.jarvis.entity.SupplierPayment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SupplierPaymentRepository extends MongoRepository<SupplierPayment, String> {
    List<SupplierPayment> findBySupplierId(String supplierId);
    List<SupplierPayment> findBySupplierIdAndPaymentDateBetween(String supplierId, LocalDate from, LocalDate to);
    List<SupplierPayment> findByPaymentDateBetween(LocalDate from, LocalDate to);
    List<SupplierPayment> findByPaymentDate(LocalDate paymentDate);
    List<SupplierPayment> findBySupplierIdAndPaymentDate(String supplierId, LocalDate paymentDate);
}
