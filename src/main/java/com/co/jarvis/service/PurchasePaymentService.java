package com.co.jarvis.service;

import com.co.jarvis.dto.PurchasePaymentDetailResponse;
import com.co.jarvis.entity.SupplierPayment;

import java.util.List;

public interface PurchasePaymentService {
    void linkPayments(String purchaseId, List<String> paymentIds, String linkedBy);
    void unlinkPayment(String paymentId);
    PurchasePaymentDetailResponse getLinkedPayments(String purchaseId);
    List<SupplierPayment> findUnlinkedBySupplier(String supplierId);

    /**
     * Aplica un pago a varias facturas con el monto indicado para cada una.
     */
    void allocatePayment(String paymentId, List<com.co.jarvis.dto.PaymentAllocationDto> allocations, String appliedBy);

    /**
     * Valida que las asignaciones sean aplicables (proveedor, saldo de cada factura y total del pago)
     * sin modificar nada.
     */
    void validateAllocations(String supplierId, java.math.BigDecimal paymentAmount,
                             List<com.co.jarvis.dto.PaymentAllocationDto> allocations);

    /**
     * Facturas del proveedor que no están pagadas (PENDIENTE o PARCIAL), más antiguas primero.
     */
    List<com.co.jarvis.dto.PurchaseInvoiceDto> findPendingInvoices(String supplierId);

    /**
     * Recalcula totalPaid y paymentStatus de una factura según sus pagos vinculados (no guarda).
     */
    void refreshPaymentStatus(com.co.jarvis.entity.PurchaseInvoice purchase);
}
