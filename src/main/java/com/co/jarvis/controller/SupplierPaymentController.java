package com.co.jarvis.controller;

import com.co.jarvis.dto.SupplierPaymentDto;
import com.co.jarvis.service.SupplierPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/supplier-payments")
public class SupplierPaymentController {

    @Autowired
    private SupplierPaymentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupplierPaymentDto> create(
            @RequestPart("metadata") SupplierPaymentDto metadata,
            @RequestPart(value = "support", required = false) MultipartFile support
    ) throws IOException {
        SupplierPaymentDto result = service.create(metadata, support);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty() || v.equalsIgnoreCase("undefined") || v.equalsIgnoreCase("null")) {
            return null;
        }
        return LocalDate.parse(v);
    }

    private static String parseSupplierId(String supplierId) {
        if (supplierId == null) return null;
        String v = supplierId.trim();
        if (v.isEmpty() || v.equalsIgnoreCase("undefined") || v.equalsIgnoreCase("null")) {
            return null;
        }
        return v;
    }

    @GetMapping
    public ResponseEntity<List<SupplierPaymentDto>> list(
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        String normalizedSupplierId = parseSupplierId(supplierId);
        return ResponseEntity.ok(service.list(normalizedSupplierId, fromDate, toDate));
    }

    @GetMapping("/{id}/support")
    public ResponseEntity<byte[]> downloadSupport(@PathVariable String id) throws IOException {
        byte[] data = service.getSupport(id);
        String contentType = service.getSupportContentType(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=soporte")
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(data);
    }
}
