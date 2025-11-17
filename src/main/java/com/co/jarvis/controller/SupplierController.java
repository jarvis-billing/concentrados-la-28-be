package com.co.jarvis.controller;

import com.co.jarvis.dto.SupplierCreateDto;
import com.co.jarvis.dto.SupplierDto;
import com.co.jarvis.dto.SupplierUpdateDto;
import com.co.jarvis.enums.SupplierStatus;
import com.co.jarvis.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService service;

    @GetMapping
    public ResponseEntity<List<SupplierDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        SupplierStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = SupplierStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) { }
        }
        List<SupplierDto> data = service.list(q, st, page, size);
        HttpHeaders headers = new HttpHeaders();
        if (page != null && size != null && size > 0) {
            long total = service.count(q, st);
            headers.add("X-Total-Count", String.valueOf(total));
        }
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<SupplierDto> create(@RequestBody SupplierCreateDto dto) {
        SupplierDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDto> update(@PathVariable String id, @RequestBody SupplierUpdateDto dto) {
        SupplierDto updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String id, @RequestBody StatusBody body) {
        SupplierStatus st = SupplierStatus.valueOf(body.getStatus().trim().toUpperCase());
        service.updateStatus(id, st);
        return ResponseEntity.noContent().build();
    }

    public static class StatusBody {
        private String status;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
