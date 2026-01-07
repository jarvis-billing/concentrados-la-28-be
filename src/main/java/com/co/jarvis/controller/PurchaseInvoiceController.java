package com.co.jarvis.controller;

import com.co.jarvis.dto.PurchaseFilterDto;
import com.co.jarvis.dto.PurchaseInvoiceDto;
import com.co.jarvis.dto.SupplierRefDto;
import com.co.jarvis.service.PurchaseInvoiceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/purchases/invoices", produces = MediaType.APPLICATION_JSON_VALUE)
public class PurchaseInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseInvoiceController.class);

    @Autowired
    private PurchaseInvoiceService service;

    /**
     * GET /api/purchases/invoices
     * Lista todas las facturas de compra con filtros opcionales
     */
    @GetMapping
    public ResponseEntity<List<PurchaseInvoiceDto>> list(
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        @RequestParam(required = false) String supplierId,
        @RequestParam(required = false) String supplierName,
        @RequestParam(required = false) String invoiceNumber
    ) {
        logger.info("PurchaseInvoiceController -> list");
        
        SupplierRefDto supplierRef = (supplierId != null || supplierName != null) 
            ? SupplierRefDto.builder().id(supplierId).name(supplierName).build() 
            : null;
        
        PurchaseFilterDto filter = PurchaseFilterDto.builder()
            .supplier(supplierRef)
            .invoiceNumber(invoiceNumber)
            .build();
        
        // Parse dates if provided
        if (dateFrom != null) {
            try {
                filter.setDateFrom(java.time.OffsetDateTime.parse(dateFrom));
            } catch (Exception e) {
                logger.warn("Invalid dateFrom format: {}", dateFrom);
            }
        }
        if (dateTo != null) {
            try {
                filter.setDateTo(java.time.OffsetDateTime.parse(dateTo));
            } catch (Exception e) {
                logger.warn("Invalid dateTo format: {}", dateTo);
            }
        }

        List<PurchaseInvoiceDto> invoices = service.list(filter);
        return ResponseEntity.ok(invoices);
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
}
