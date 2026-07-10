package com.co.jarvis.controller;

import com.co.jarvis.dto.AddItemsRequest;
import com.co.jarvis.dto.BulkLastCostItem;
import com.co.jarvis.dto.BulkLastCostRequest;
import com.co.jarvis.dto.CostHistoryEntry;
import com.co.jarvis.dto.LinkPaymentsRequest;
import com.co.jarvis.dto.PagedResponse;
import com.co.jarvis.dto.PurchaseFilterDto;
import com.co.jarvis.dto.PurchaseInvoiceDto;
import com.co.jarvis.dto.PurchaseLastCostInfo;
import com.co.jarvis.dto.PurchasePaymentDetailResponse;
import com.co.jarvis.dto.SupplierRefDto;
import com.co.jarvis.service.PurchaseInvoiceService;
import com.co.jarvis.service.PurchasePaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/purchases/invoices", produces = MediaType.APPLICATION_JSON_VALUE)
public class PurchaseInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseInvoiceController.class);

    @Autowired
    private PurchaseInvoiceService service;

    @Autowired
    private PurchasePaymentService purchasePaymentService;

    /**
     * GET /api/purchases/invoices
     * Lista facturas de compra con filtros y paginación server-side.
     *
     * @param createdAtFrom  fecha inicio ingreso (yyyy-MM-dd)
     * @param createdAtTo    fecha fin ingreso   (yyyy-MM-dd)
     * @param supplierId     ID del proveedor
     * @param productBarcode código de barras de presentación en los ítems
     * @param invoiceNumber  número de factura (búsqueda parcial)
     * @param page           página (0-indexed, default 0)
     * @param size           tamaño de página (default 20)
     */
    @GetMapping
    public ResponseEntity<PagedResponse<PurchaseInvoiceDto>> list(
        @RequestParam(required = false) String createdAtFrom,
        @RequestParam(required = false) String createdAtTo,
        @RequestParam(required = false) String supplierId,
        @RequestParam(required = false) String productBarcode,
        @RequestParam(required = false) String invoiceNumber,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        logger.info("PurchaseInvoiceController -> list: page={}, size={}", page, size);

        SupplierRefDto supplierRef = supplierId != null
                ? SupplierRefDto.builder().id(supplierId).build()
                : null;

        PurchaseFilterDto filter = PurchaseFilterDto.builder()
                .supplier(supplierRef)
                .invoiceNumber(invoiceNumber)
                .productBarcode(productBarcode)
                .build();

        if (createdAtFrom != null && !createdAtFrom.isBlank()) {
            try { filter.setCreatedAtFrom(LocalDate.parse(createdAtFrom)); }
            catch (DateTimeParseException e) { logger.warn("Invalid createdAtFrom: {}", createdAtFrom); }
        }
        if (createdAtTo != null && !createdAtTo.isBlank()) {
            try { filter.setCreatedAtTo(LocalDate.parse(createdAtTo)); }
            catch (DateTimeParseException e) { logger.warn("Invalid createdAtTo: {}", createdAtTo); }
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));
        return ResponseEntity.ok(service.listPaged(filter, pageable));
    }

    /**
     * GET /api/purchases/invoices/{id}
     * Obtiene una factura de compra por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseInvoiceDto> findById(@PathVariable String id) {
        logger.info("PurchaseInvoiceController -> findById: {}", id);
        
        PurchaseInvoiceDto invoice = service.findById(id);
        if (invoice == null) {
            logger.warn("PurchaseInvoiceController -> findById -> Factura no encontrada: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        return ResponseEntity.ok(invoice);
    }

    /**
     * POST /api/purchases/invoices
     * Crea una nueva factura de compra y actualiza el stock de productos
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PurchaseInvoiceDto> create(@Valid @RequestBody PurchaseInvoiceDto dto) {
        logger.info("PurchaseInvoiceController -> create");
        
        PurchaseInvoiceDto createdInvoice = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdInvoice);
    }

    /**
     * PUT /api/purchases/invoices/{id}
     * Actualiza una factura de compra existente y ajusta el stock de productos
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PurchaseInvoiceDto> update(
        @PathVariable String id,
        @Valid @RequestBody PurchaseInvoiceDto dto
    ) {
        logger.info("PurchaseInvoiceController -> update: {}", id);
        
        PurchaseInvoiceDto updatedInvoice = service.update(id, dto);
        return ResponseEntity.ok(updatedInvoice);
    }

    /**
     * DELETE /api/purchases/invoices/{id}
     * Elimina una factura de compra
     * Nota: No revierte el stock automáticamente, considerar implementar lógica adicional si es necesario
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        logger.info("PurchaseInvoiceController -> delete: {}", id);
        
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/purchases/invoices/{id}/items
     * Agrega nuevos items a una factura de compra existente.
     * Los items existentes NO se modifican ni eliminan.
     * Recalcula el total de la factura y actualiza el stock de productos.
     */
    @PostMapping(value = "/{id}/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PurchaseInvoiceDto> addItems(
        @PathVariable String id,
        @Valid @RequestBody AddItemsRequest request
    ) {
        logger.info("PurchaseInvoiceController -> addItems: {}", id);
        
        if (request.getItems() == null || request.getItems().isEmpty()) {
            logger.warn("PurchaseInvoiceController -> addItems -> No se proporcionaron items");
            return ResponseEntity.badRequest().build();
        }
        
        PurchaseInvoiceDto updatedInvoice = service.addItems(id, request.getItems());
        return ResponseEntity.ok(updatedInvoice);
    }

    /**
     * GET /api/purchases/invoices/last-cost?presentationId=770123456789
     * Devuelve el costo de la última compra registrada para una presentación dada.
     */
    @GetMapping("/last-cost")
    public ResponseEntity<PurchaseLastCostInfo> getLastCost(
            @RequestParam String presentationId) {
        logger.info("PurchaseInvoiceController -> getLastCost: presentationId={}", presentationId);

        PurchaseLastCostInfo info = service.getLastCost(presentationId);
        if (info == null) {
            return ResponseEntity.ok().body(null);
        }
        return ResponseEntity.ok(info);
    }

    /**
     * GET /api/purchases/invoices/cost-history?presentationId=770123456789&fromDate=2025-01-01&toDate=2025-12-31
     * Devuelve el historial completo de compras de una presentación.
     */
    @GetMapping("/cost-history")
    public ResponseEntity<List<CostHistoryEntry>> getCostHistory(
            @RequestParam String presentationId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        logger.info("PurchaseInvoiceController -> getCostHistory: presentationId={}, fromDate={}, toDate={}",
                presentationId, fromDate, toDate);

        LocalDate from = null;
        LocalDate to = null;

        if (fromDate != null && !fromDate.isBlank()) {
            try {
                from = LocalDate.parse(fromDate);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid fromDate format: {}", fromDate);
            }
        }
        if (toDate != null && !toDate.isBlank()) {
            try {
                to = LocalDate.parse(toDate);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid toDate format: {}", toDate);
            }
        }

        List<CostHistoryEntry> history = service.getCostHistory(presentationId, from, to);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/link-payments")
    public ResponseEntity<Void> linkPayments(
            @PathVariable String id,
            @RequestBody LinkPaymentsRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String linkedBy) {
        logger.info("PurchaseInvoiceController -> linkPayments: purchaseId={}", id);
        purchasePaymentService.linkPayments(id, request.getPaymentIds(), linkedBy);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<PurchasePaymentDetailResponse> getLinkedPayments(@PathVariable String id) {
        logger.info("PurchaseInvoiceController -> getLinkedPayments: purchaseId={}", id);
        return ResponseEntity.ok(purchasePaymentService.getLinkedPayments(id));
    }

    @PostMapping("/last-cost/bulk")
    public ResponseEntity<List<BulkLastCostItem>> bulkLastCost(
            @Valid @RequestBody BulkLastCostRequest request) {
        logger.info("PurchaseInvoiceController -> bulkLastCost: {} barcodes", request.barcodes().size());
        return ResponseEntity.ok(service.bulkGetLastCost(request.barcodes()));
    }
}
