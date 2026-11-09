package com.co.jarvis.service.impl;

import com.co.jarvis.dto.PaymentAllocationDto;
import com.co.jarvis.dto.PaymentDetailDto;
import com.co.jarvis.dto.PurchaseInvoiceDto;
import com.co.jarvis.dto.PurchasePaymentDetailResponse;
import com.co.jarvis.entity.LinkedPayment;
import com.co.jarvis.entity.PaymentApplication;
import com.co.jarvis.entity.PurchaseInvoice;
import com.co.jarvis.entity.SupplierPayment;
import com.co.jarvis.enums.EPurchaseInvoiceStatus;
import com.co.jarvis.repository.PurchaseInvoiceRepository;
import com.co.jarvis.repository.SupplierPaymentRepository;
import com.co.jarvis.service.PurchasePaymentService;
import com.co.jarvis.util.mappers.GenericMapper;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchasePaymentServiceImpl implements PurchasePaymentService {

    private static final BigDecimal TOLERANCE = BigDecimal.ONE;

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final MongoTemplate mongoTemplate;

    private final GenericMapper<PurchaseInvoice, PurchaseInvoiceDto> invoiceMapper =
            new GenericMapper<>(PurchaseInvoice.class, PurchaseInvoiceDto.class);

    /**
     * Vincula pagos/anticipos existentes a una factura. Cada pago aporta su saldo disponible
     * hasta cubrir el saldo de la factura; lo que sobre sigue como anticipo.
     */
    @Override
    public void linkPayments(String purchaseId, List<String> paymentIds, String linkedBy) {
        log.info("PurchasePaymentServiceImpl -> linkPayments: purchaseId={}, paymentIds={}", purchaseId, paymentIds);

        PurchaseInvoice purchase = findPurchase(purchaseId);
        Map<String, SupplierPayment> byId = new HashMap<>();
        supplierPaymentRepository.findAllById(paymentIds).forEach(p -> byId.put(p.getId(), p));

        for (String paymentId : paymentIds) {
            SupplierPayment p = byId.get(paymentId);
            if (p == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no existe: " + paymentId);
            }
            validatePaymentForPurchase(p, purchase);
        }

        // Aplicar en el orden recibido (el más antiguo primero si el front así lo envía)
        for (String paymentId : paymentIds) {
            BigDecimal balance = balanceOf(purchase);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) break;
            SupplierPayment payment = byId.get(paymentId);
            BigDecimal toApply = availableOf(payment).min(balance);
            if (toApply.compareTo(BigDecimal.ZERO) <= 0) continue;
            apply(payment, purchase, toApply, linkedBy);
        }

        purchaseInvoiceRepository.save(purchase);
        log.info("Payments linked. purchaseId={}, totalPaid={}, status={}",
                purchaseId, purchase.getTotalPaid(), purchase.getPaymentStatus());
    }

    @Override
    public void validateAllocations(String supplierId, BigDecimal paymentAmount, List<PaymentAllocationDto> allocations) {
        loadAndValidateAllocations(supplierId, paymentAmount, allocations);
    }

    @Override
    public void allocatePayment(String paymentId, List<PaymentAllocationDto> allocations, String appliedBy) {
        log.info("PurchasePaymentServiceImpl -> allocatePayment: paymentId={}, allocations={}", paymentId, allocations);

        SupplierPayment payment = supplierPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no existe"));
        if ("ANULADO".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago está anulado");
        }

        Map<String, PurchaseInvoice> purchases =
                loadAndValidateAllocations(payment.getSupplierId(), availableOf(payment), allocations);

        for (PaymentAllocationDto a : allocations) {
            PurchaseInvoice purchase = purchases.get(a.getPurchaseId());
            apply(payment, purchase, a.getAmount(), appliedBy);
            purchaseInvoiceRepository.save(purchase);
        }
    }

    @Override
    public void unlinkPayment(String paymentId) {
        log.info("PurchasePaymentServiceImpl -> unlinkPayment: paymentId={}", paymentId);

        SupplierPayment payment = supplierPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no existe"));

        if ("ANULADO".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede desvincular un pago anulado");
        }
        List<PaymentApplication> applications = applicationsOf(payment);
        if (applications.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago no está aplicado a ninguna factura");
        }

        // Quitar el pago de cada factura donde se aplicó y recalcular su estado
        Set<String> purchaseIds = applications.stream()
                .map(PaymentApplication::getPurchaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (PurchaseInvoice purchase : purchaseInvoiceRepository.findAllById(purchaseIds)) {
            if (purchase.getLinkedPayments() != null) {
                purchase.getLinkedPayments().removeIf(lp -> paymentId.equals(lp.getPaymentId()));
            }
            refreshPaymentStatus(purchase);
            purchase.setUpdatedAt(OffsetDateTime.now());
            purchaseInvoiceRepository.save(purchase);
        }

        // El pago vuelve a ser anticipo completo
        payment.setApplications(new ArrayList<>());
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

        PurchaseInvoice purchase = findPurchase(purchaseId);

        List<LinkedPayment> linked = purchase.getLinkedPayments() != null ? purchase.getLinkedPayments() : List.of();
        Map<String, SupplierPayment> payments = new HashMap<>();
        supplierPaymentRepository.findAllById(linked.stream().map(LinkedPayment::getPaymentId).toList())
                .forEach(p -> payments.put(p.getId(), p));

        List<PaymentDetailDto> details = new ArrayList<>();
        for (LinkedPayment lp : linked) {
            SupplierPayment sp = payments.get(lp.getPaymentId());
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
        query.with(Sort.by(Sort.Direction.ASC, "paymentDate"));
        // remainingAmount puede estar guardado como string: se filtra en memoria
        return mongoTemplate.find(query, SupplierPayment.class).stream()
                .filter(p -> availableOf(p).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Override
    public List<PurchaseInvoiceDto> findPendingInvoices(String supplierId) {
        log.info("PurchasePaymentServiceImpl -> findPendingInvoices: supplierId={}", supplierId);
        Query query = new Query();
        query.addCriteria(Criteria.where("supplier.id").is(supplierId));
        query.addCriteria(Criteria.where("status").ne(EPurchaseInvoiceStatus.CANCELLED));
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("payment_status").in("PENDIENTE", "PARCIAL"),
                Criteria.where("payment_status").exists(false)));
        query.fields().exclude("items");
        query.with(Sort.by(Sort.Direction.ASC, "invoice_date"));
        return mongoTemplate.find(query, PurchaseInvoice.class).stream()
                .filter(p -> balanceOf(p).compareTo(TOLERANCE) > 0)
                .map(invoiceMapper::mapToDto)
                .toList();
    }

    @Override
    public void refreshPaymentStatus(PurchaseInvoice purchase) {
        BigDecimal paid = purchase.getLinkedPayments() != null
                ? purchase.getLinkedPayments().stream()
                    .map(LinkedPayment::getAppliedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        paid = paid.add(nz(purchase.getLegacyPaidAmount()));
        purchase.setTotalPaid(paid);
        purchase.setPaymentStatus(calculatePaymentStatus(purchase.getTotalAmount(), paid));
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, PurchaseInvoice> loadAndValidateAllocations(String supplierId, BigDecimal available,
                                                                    List<PaymentAllocationDto> allocations) {
        if (allocations == null || allocations.isEmpty()) return Map.of();

        Set<String> ids = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentAllocationDto a : allocations) {
            if (a.getPurchaseId() == null || a.getAmount() == null || a.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada factura seleccionada debe tener un monto mayor a 0");
            }
            if (!ids.add(a.getPurchaseId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una factura está repetida en la selección");
            }
            total = total.add(a.getAmount());
        }
        if (available == null || total.subtract(available).compareTo(TOLERANCE) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El total aplicado a facturas (" + total.toPlainString() + ") supera el valor del pago");
        }

        Map<String, PurchaseInvoice> purchases = new HashMap<>();
        purchaseInvoiceRepository.findAllById(ids).forEach(p -> purchases.put(p.getId(), p));
        for (PaymentAllocationDto a : allocations) {
            PurchaseInvoice p = purchases.get(a.getPurchaseId());
            if (p == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura de compra no existe: " + a.getPurchaseId());
            }
            String purchaseSupplier = p.getSupplier() != null ? p.getSupplier().getId() : null;
            if (!Objects.equals(supplierId, purchaseSupplier)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La factura " + p.getInvoiceNumber() + " no pertenece a este proveedor");
            }
            if (p.getStatus() == EPurchaseInvoiceStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La factura " + p.getInvoiceNumber() + " está anulada");
            }
            if (a.getAmount().subtract(balanceOf(p)).compareTo(TOLERANCE) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El monto aplicado a la factura " + p.getInvoiceNumber() + " supera su saldo pendiente ("
                                + balanceOf(p).toPlainString() + ")");
            }
        }
        return purchases;
    }

    private void validatePaymentForPurchase(SupplierPayment p, PurchaseInvoice purchase) {
        String supplierIdInPurchase = purchase.getSupplier() != null ? purchase.getSupplier().getId() : null;
        if (!Objects.equals(p.getSupplierId(), supplierIdInPurchase)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El pago " + p.getId() + " no pertenece al proveedor de esta compra");
        }
        if ("ANULADO".equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago " + p.getId() + " está anulado");
        }
        if (availableOf(p).compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pago " + p.getId() + " no tiene saldo disponible");
        }
    }

    /**
     * Aplica {@code amount} del pago a la factura actualizando ambos lados.
     * Guarda el pago; la factura la guarda quien llama.
     */
    private void apply(SupplierPayment payment, PurchaseInvoice purchase, BigDecimal amount, String appliedBy) {
        OffsetDateTime now = OffsetDateTime.now();

        // Pago: registrar aplicación (sumando si ya se había aplicado a esta factura)
        List<PaymentApplication> applications = new ArrayList<>(applicationsOf(payment));
        PaymentApplication existing = applications.stream()
                .filter(ap -> purchase.getId().equals(ap.getPurchaseId()))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setAmount(existing.getAmount().add(amount));
            existing.setAppliedAt(now);
        } else {
            applications.add(PaymentApplication.builder()
                    .purchaseId(purchase.getId())
                    .invoiceNumber(purchase.getInvoiceNumber())
                    .amount(amount)
                    .appliedAt(now)
                    .appliedBy(appliedBy)
                    .build());
        }
        BigDecimal applied = applications.stream()
                .map(PaymentApplication::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = nz(payment.getAmount()).subtract(applied).max(BigDecimal.ZERO);

        payment.setApplications(applications);
        payment.setAppliedAmount(applied);
        payment.setRemainingAmount(remaining);
        payment.setLinkedPurchaseId(purchase.getId());
        payment.setLinkedAt(now);
        payment.setLinkedBy(appliedBy);
        payment.setStatus(remaining.compareTo(BigDecimal.ZERO) == 0 ? "VINCULADO" : "PARCIAL");
        payment.setUpdatedAt(now);
        supplierPaymentRepository.save(payment);

        // Factura: registrar pago vinculado (sumando si el mismo pago ya estaba)
        List<LinkedPayment> linked = purchase.getLinkedPayments() != null
                ? new ArrayList<>(purchase.getLinkedPayments()) : new ArrayList<>();
        LinkedPayment lp = linked.stream()
                .filter(l -> payment.getId().equals(l.getPaymentId()))
                .findFirst().orElse(null);
        if (lp != null) {
            lp.setAppliedAmount(nz(lp.getAppliedAmount()).add(amount));
        } else {
            linked.add(LinkedPayment.builder()
                    .paymentId(payment.getId())
                    .appliedAmount(amount)
                    .paymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null)
                    .method(payment.getMethod() != null ? payment.getMethod().name() : null)
                    .reference(payment.getReference())
                    .build());
        }
        purchase.setLinkedPayments(linked);
        refreshPaymentStatus(purchase);
        purchase.setUpdatedAt(now);
    }

    /**
     * Aplicaciones del pago. Los pagos vinculados antes de existir "applications"
     * solo tienen linkedPurchaseId + appliedAmount: se interpretan como una aplicación.
     */
    private List<PaymentApplication> applicationsOf(SupplierPayment payment) {
        if (payment.getApplications() != null && !payment.getApplications().isEmpty()) {
            return payment.getApplications();
        }
        if (payment.getLinkedPurchaseId() != null && nz(payment.getAppliedAmount()).compareTo(BigDecimal.ZERO) > 0) {
            return List.of(PaymentApplication.builder()
                    .purchaseId(payment.getLinkedPurchaseId())
                    .amount(payment.getAppliedAmount())
                    .appliedAt(payment.getLinkedAt())
                    .appliedBy(payment.getLinkedBy())
                    .build());
        }
        return List.of();
    }

    private BigDecimal availableOf(SupplierPayment payment) {
        return payment.getRemainingAmount() != null ? payment.getRemainingAmount() : nz(payment.getAmount());
    }

    private BigDecimal balanceOf(PurchaseInvoice purchase) {
        return nz(purchase.getTotalAmount()).subtract(nz(purchase.getTotalPaid()));
    }

    private PurchaseInvoice findPurchase(String purchaseId) {
        return purchaseInvoiceRepository.findById(purchaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no existe: " + purchaseId));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String calculatePaymentStatus(BigDecimal total, BigDecimal paid) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return "PENDIENTE";
        if (paid == null || paid.compareTo(BigDecimal.ZERO) == 0) return "PENDIENTE";
        BigDecimal diff = total.subtract(paid);
        if (diff.abs().compareTo(TOLERANCE) <= 0) return "PAGADO";
        if (paid.compareTo(total) > 0) return "SOBREPAGADO";
        return "PARCIAL";
    }
}
