package com.co.jarvis.service.impl;

import com.co.jarvis.dto.ExpenseDto;
import com.co.jarvis.dto.ExpensePageDto;
import com.co.jarvis.entity.Expense;
import com.co.jarvis.repository.ExpenseRepository;
import com.co.jarvis.service.ExpenseService;
import com.co.jarvis.util.exception.FieldsException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public ExpenseDto create(ExpenseDto dto) {
        validate(dto);

        OffsetDateTime dateTime = dto.getDateTimeRecord();
        if (dateTime == null) {
            // Set now in Bogota with -05:00 offset
            dateTime = OffsetDateTime.now(BOGOTA);
        }

        Expense entity = toEntity(dto);
        entity.setId(null);
        entity.setDateTimeRecord(dateTime);
        Expense saved = expenseRepository.save(entity);
        return toDto(saved);
    }

    @Override
    public ExpenseDto update(String id, ExpenseDto dto) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));

        validate(dto);

        // Merge fields
        if (dto.getDateTimeRecord() != null) {
            existing.setDateTimeRecord(dto.getDateTimeRecord());
        }
        existing.setAmount(dto.getAmount());
        existing.setPaymentMethod(dto.getPaymentMethod());
        existing.setCategory(dto.getCategory());
        existing.setDescription(dto.getDescription());
        existing.setReference(dto.getReference());
        existing.setSource(dto.getSource());
        if (dto.getCreatedBy() != null && !dto.getCreatedBy().isBlank()) {
            existing.setCreatedBy(dto.getCreatedBy());
        }

        Expense saved = expenseRepository.save(existing);
        return toDto(saved);
    }

    @Override
    public void delete(String id) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
        expenseRepository.deleteById(existing.getId());
    }

    @Override
    public ExpensePageDto list(String fromDate, String toDate, String category, Integer page, Integer size, String sort) {
        int p = page != null ? page : 0;
        int s = size != null ? size : 50;
        String sortParam = (sort != null && !sort.isBlank()) ? sort : "dateTimeRecord,desc";
        Sort springSort = toSort(sortParam);

        Query query = new Query();

        // Date range filters inclusive
        if (StringUtils.hasText(fromDate) || StringUtils.hasText(toDate)) {
            Criteria dateCriteria = Criteria.where("dateTimeRecord");
            if (StringUtils.hasText(fromDate)) {
                LocalDate fd = LocalDate.parse(fromDate);
                OffsetDateTime from = fd.atStartOfDay(BOGOTA).toOffsetDateTime();
                dateCriteria = dateCriteria.gte(from);
            }
            if (StringUtils.hasText(toDate)) {
                LocalDate td = LocalDate.parse(toDate);
                OffsetDateTime to = td.atTime(LocalTime.of(23,59,59)).atZone(BOGOTA).toOffsetDateTime();
                dateCriteria = dateCriteria.lte(to);
            }
            query.addCriteria(dateCriteria);
        }

        if (StringUtils.hasText(category)) {
            String regex = ".*" + category.trim() + ".*";
            query.addCriteria(Criteria.where("category").regex(regex, "i"));
        }

        long total = mongoTemplate.count(query, Expense.class);
        query.with(springSort);
        query.skip((long) p * s);
        query.limit(s);

        List<Expense> data = mongoTemplate.find(query, Expense.class);
        List<ExpenseDto> items = data.stream().map(this::toDto).collect(Collectors.toList());
        long totalPages = (long) Math.ceil((double) total / (double) s);

        return ExpensePageDto.builder()
                .items(items)
                .page(p)
                .size(s)
                .totalItems(total)
                .totalPages((int) totalPages)
                .build();
    }

    @Override
    public List<String> listCategories() {
        Query query = new Query();
        return mongoTemplate.findDistinct(query, "category", Expense.class, String.class)
                .stream()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    private void validate(ExpenseDto dto) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.put("amount", "Debe ser mayor a 0");
        }
        if (dto.getPaymentMethod() == null) {
            errors.put("paymentMethod", "Requerido");
        }
        if (!StringUtils.hasText(dto.getCategory())) {
            errors.put("category", "Requerido");
        } else if (dto.getCategory().length() > 50) {
            errors.put("category", "Máximo 50 caracteres");
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            errors.put("description", "Requerido");
        } else if (dto.getDescription().length() > 200) {
            errors.put("description", "Máximo 200 caracteres");
        }

        if (!errors.isEmpty()) {
            throw new FieldsException("Validation failed", errors);
        }
    }

    private Sort toSort(String sortParam) {
        try {
            String[] parts = sortParam.split(",");
            String prop = parts[0];
            Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
            return Sort.by(dir, prop);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "dateTimeRecord");
        }
    }

    private ExpenseDto toDto(Expense e) {
        return ExpenseDto.builder()
                .id(e.getId())
                .dateTimeRecord(e.getDateTimeRecord())
                .amount(e.getAmount())
                .paymentMethod(e.getPaymentMethod())
                .category(e.getCategory())
                .description(e.getDescription())
                .reference(e.getReference())
                .source(e.getSource())
                .createdBy(e.getCreatedBy())
                .build();
    }

    private Expense toEntity(ExpenseDto d) {
        return Expense.builder()
                .id(d.getId())
                .dateTimeRecord(d.getDateTimeRecord())
                .amount(d.getAmount())
                .paymentMethod(d.getPaymentMethod())
                .category(d.getCategory())
                .description(d.getDescription())
                .reference(d.getReference())
                .source(d.getSource())
                .createdBy(d.getCreatedBy())
                .build();
    }
}
