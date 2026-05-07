package com.co.jarvis.service.impl;

import com.co.jarvis.dto.PaymentDetailDto;
import com.co.jarvis.dto.PurchasePaymentDetailResponse;
import com.co.jarvis.entity.LinkedPayment;
import com.co.jarvis.entity.PurchaseInvoice;
import com.co.jarvis.entity.SupplierPayment;
import com.co.jarvis.repository.PurchaseInvoiceRepository;
import com.co.jarvis.repository.SupplierPaymentRepository;
import com.co.jarvis.service.PurchasePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchasePaymentServiceImpl implements PurchasePaymentService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public void linkPayments(String purchaseId, List<String> paymentIds, String linkedBy) {
        log.info("PurchasePaymentServiceImpl -> linkPayments: purchaseId={}, paymentIds={}", purchaseId, paymentIds);

        PurchaseInvoice purchase = purchaseInvoiceRepository.findById(purchaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no existe: " + purchaseId));

        List<SupplierPayment> payments = new ArrayList<>();
        supplierPaymentRepository.findAllById(paymentIds).forEach(payments::add);

        // 1. Validaciones previas
        for (SupplierPayment p : payments) {
            String supplierIdInPurchase = purchase.getSupplier() != null ? purchase.getSupplier().getId() : null;
            if (!p.getSupplierId().equals(supplierIdInPurchase)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El pago " + p.getId() + " no pertenece al proveedor de esta compra");
            }
            if ("ANULADO".equals(p.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El pago " + p.getId() + " está anulado");
            }
            if ("VINCULADO".equals(p.getStatus()) && !purchaseId.equals(p.getLinkedPurchaseId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El pago " + p.getId() + " ya está vinculado a otra compra");
            }
        }

        // 2. Aplicar cada pago
        BigDecimal purchaseTotal = purchase.getTotalAmount() != null ? purchase.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal currentPaid = purchase.getTotalPaid() != null ? purchase.getTotalPaid() : BigDecimal.ZERO;
        BigDecimal remainingToPay = purchaseTotal.subtract(currentPaid);
        BigDecimal newPaid = BigDecimal.ZERO;

        List<LinkedPayment> linkedPayments = purchase.getLinkedPayments() != null
                ? new ArrayList<>(purchase.getLinkedPayments())
                : new ArrayList<>();

        for (SupplierPayment payment : payments) {
            BigDecimal available = payment.getRemainingAmount() != null
                    ? payment.getRemainingAmount()
                    : payment.getAmount();

            if (available == null || available.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal toApply;
            if (remainingToPay.compareTo(BigDecimal.ZERO) > 0) {
                toApply = available.min(remainingToPay);
            } else {
                toApply = available; // sobrepago permitido
            }

            // Actualizar SupplierPayment
            BigDecimal applied = payment.getAppliedAmount() != null ? payment.getAppliedAmount() : BigDecimal.ZERO;
            payment.setAppliedAmount(applied.add(toApply));
            payment.setRemainingAmount(available.subtract(toApply));
            payment.setLinkedPurchaseId(purchaseId);
            payment.setLinkedAt(OffsetDateTime.now());
            payment.setLinkedBy(linkedBy);
            payment.setStatus(payment.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0 ? "VINCULADO" : "PARCIAL");
            payment.setUpdatedAt(OffsetDateTime.now());
            supplierPaymentRepository.save(payment);

            // Agregar LinkedPayment embebido (evitar duplicados del mismo pago)
            linkedPayments.removeIf(lp -> payment.getId().equals(lp.getPaymentId()));
            linkedPayments.add(LinkedPayment.builder()
                    .paymentId(payment.getId())
                    .appliedAmount(toApply)
                    .paymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null)
                    .method(payment.getMethod() != null ? payment.getMethod().name() : null)
                    .reference(payment.getReference())
                    .build());

            newPaid = newPaid.add(toApply);
            remainingToPay = remainingToPay.subtract(toApply);
        }

        // 3. Actualizar PurchaseInvoice
        BigDecimal updatedTotalPaid = currentPaid.add(newPaid);
        purchase.setTotalPaid(updatedTotalPaid);
        purchase.setPaymentStatus(calculatePaymentStatus(purchaseTotal, updatedTotalPaid));
        purchase.setLinkedPayments(linkedPayments);
        purchase.setUpdatedAt(OffsetDateTime.now());
        purchaseInvoiceRepository.save(purchase);

        log.info("Payments linked successfully. purchaseId={}, totalPaid={}, status={}",
                purchaseId, updatedTotalPaid, purchase.getPaymentStatus());
    }

    @Override
    public void unlinkPayment(String paymentId) {
        log.info("PurchasePaymentServiceImpl -> unlinkPayment: paymentId={}", paymentId);

        SupplierPayment payment = supplierPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no existe"));

        if ("ADELANTO".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago ya está desvinculado");
        }
        if ("ANULADO".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede desvincular un pago anulado");
        }

        String purchaseId = payment.getLinkedPurchaseId();
        if (purchaseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago no tiene compra vinculada");
        }

        PurchaseInvoice purchase = purchaseInvoiceRepository.findById(purchaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra vinculada no existe"));

        // 1. Remover de lista embebida
        if (purchase.getLinkedPayments() != null) {
            purchase.getLinkedPayments().removeIf(lp -> paymentId.equals(lp.getPaymentId()));
        }

        // 2. Recalcular totalPaid
        BigDecimal sumApplied = purchase.getLinkedPayments() != null
                ? purchase.getLinkedPayments().stream()
                    .map(LinkedPayment::getAppliedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        purchase.setTotalPaid(sumApplied);
        purchase.setPaymentStatus(calculatePaymentStatus(purchase.getTotalAmount(), sumApplied));
        purchase.setUpdatedAt(OffsetDateTime.now());
        purchaseInvoiceRepository.save(purchase);

        // 3. Resetear pago a ADELANTO
        payment.setStatus("ADELANTO");
        payment.setLinkedPurchaseId(null);
        payment.setLinkedAt(null);
        payment.setLinkedBy(null);
        payment.setAppliedAmount(BigDecimal.ZERO);
        payment.setRemainingAmount(payment.getAmount());
        payment.setUpdatedAt(OffsetDateTime.now());
        supplierPaymentRepository.save(payment);

        log.info("Payment unlinked successfully. paymentId={}", paymentId);
    }

    @Override
    public PurchasePaymentDetailResponse getLinkedPayments(String purchaseId) {
        log.info("PurchasePaymentServiceImpl -> getLinkedPayments: purchaseId={}", purchaseId);

        PurchaseInvoice purchase = purchaseInvoiceRepository.findById(purchaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no existe"));

        List<PaymentDetailDto> details = new ArrayList<>();
        if (purchase.getLinkedPayments() != null) {
            for (LinkedPayment lp : purchase.getLinkedPayments()) {
                SupplierPayment sp = supplierPaymentRepository.findById(lp.getPaymentId()).orElse(null);
                details.add(PaymentDetailDto.builder()
                        .paymentId(lp.getPaymentId())
                        .appliedAmount(lp.getAppliedAmount())
                        .paymentDate(lp.getPaymentDate())
                        .method(lp.getMethod())
                        .reference(lp.getReference())
                        .bankAccountName(sp != null ? sp.getBankAccountName() : null)
                        .originalAmount(sp != null ? sp.getAmount() : null)
                        .build());
            }
        }

        return PurchasePaymentDetailResponse.builder()
                .purchaseId(purchaseId)
                .purchaseTotal(purchase.getTotalAmount())
                .totalPaid(purchase.getTotalPaid())
                .paymentStatus(purchase.getPaymentStatus())
                .payments(details)
                .build();
    }

    @Override
    public List<SupplierPayment> findUnlinkedBySupplier(String supplierId) {
        log.info("PurchasePaymentServiceImpl -> findUnlinkedBySupplier: supplierId={}", supplierId);
        Query query = new Query();
        query.addCriteria(Criteria.where("supplierId").is(supplierId));
        query.addCriteria(Criteria.where("status").in("ADELANTO", "PARCIAL"));
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("remainingAmount").gt(BigDecimal.ZERO),
                Criteria.where("remainingAmount").exists(false)
        ));
        query.with(Sort.by(Sort.Direction.DESC, "paymentDate"));
        return mongoTemplate.find(query, SupplierPayment.class);
    }

    private String calculatePaymentStatus(BigDecimal total, BigDecimal paid) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return "PENDIENTE";
        if (paid == null || paid.compareTo(BigDecimal.ZERO) == 0) return "PENDIENTE";
        BigDecimal diff = total.subtract(paid);
        BigDecimal tolerance = new BigDecimal("1");
        if (diff.abs().compareTo(tolerance) <= 0) return "PAGADO";
        if (paid.compareTo(total) > 0) return "SOBREPAGADO";
        return "PARCIAL";
    }
}
