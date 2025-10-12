package com.co.jarvis.controller;

import com.co.jarvis.dto.ExpenseDto;
import com.co.jarvis.dto.ExpensePageDto;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseDto> create(@Valid @RequestBody ExpenseDto dto) {
        // Set createdBy from JWT if not provided
        if (dto.getCreatedBy() == null || dto.getCreatedBy().isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDto user) {
                dto.setCreatedBy(user.getNumberIdentity());
            }
        }
        ExpenseDto result = expenseService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<ExpensePageDto> list(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false, defaultValue = "dateTimeRecord,desc") String sort
    ) {
        ExpensePageDto result = expenseService.list(fromDate, toDate, category, page, size, sort);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(expenseService.listCategories());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDto> update(@PathVariable String id, @Valid @RequestBody ExpenseDto dto) {
        if (dto.getCreatedBy() == null || dto.getCreatedBy().isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDto user) {
                dto.setCreatedBy(user.getNumberIdentity());
            }
        }
        ExpenseDto result = expenseService.update(id, dto);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
