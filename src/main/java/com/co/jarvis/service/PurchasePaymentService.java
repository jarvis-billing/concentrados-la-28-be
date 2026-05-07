package com.co.jarvis.service;

import com.co.jarvis.dto.PurchasePaymentDetailResponse;
import com.co.jarvis.entity.SupplierPayment;

import java.util.List;

public interface PurchasePaymentService {
    void linkPayments(String purchaseId, List<String> paymentIds, String linkedBy);
    void unlinkPayment(String paymentId);
    PurchasePaymentDetailResponse getLinkedPayments(String purchaseId);
    List<SupplierPayment> findUnlinkedBySupplier(String supplierId);
}
