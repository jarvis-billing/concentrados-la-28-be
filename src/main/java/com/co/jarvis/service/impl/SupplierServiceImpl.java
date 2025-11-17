package com.co.jarvis.service.impl;

import com.co.jarvis.dto.SupplierCreateDto;
import com.co.jarvis.dto.SupplierDto;
import com.co.jarvis.dto.SupplierUpdateDto;
import com.co.jarvis.entity.Supplier;
import com.co.jarvis.enums.DocumentType;
import com.co.jarvis.enums.SupplierStatus;
import com.co.jarvis.repository.SupplierRepository;
import com.co.jarvis.service.SupplierService;
import com.co.jarvis.util.exception.DuplicateRecordException;
import com.co.jarvis.util.exception.FieldsException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.mappers.GenericMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final GenericMapper<Supplier, SupplierDto> mapper = new GenericMapper<>(Supplier.class, SupplierDto.class);

    @Override
    public List<SupplierDto> list(String q, SupplierStatus status, Integer page, Integer size) {
        Query query = buildFilterQuery(q, status);
        query.with(Sort.by(Sort.Direction.ASC, "name"));
        if (page != null && size != null && size > 0) {
            query.skip((long) page * size);
            query.limit(size);
        }
        List<Supplier> data = mongoTemplate.find(query, Supplier.class);
        return mapper.mapToDtoList(data);
    }

    @Override
    public long count(String q, SupplierStatus status) {
        Query query = buildFilterQuery(q, status);
        return mongoTemplate.count(query, Supplier.class);
    }

    private Query buildFilterQuery(String q, SupplierStatus status) {
        Query query = new Query();
        if (StringUtils.hasText(q)) {
            String escaped = Pattern.quote(q.trim());
            Criteria nameLike = Criteria.where("name").regex(".*" + escaped + ".*", "i");
            Criteria idLike = Criteria.where("idNumber").regex(".*" + escaped + ".*", "i");
            query.addCriteria(new Criteria().orOperator(nameLike, idLike));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        return query;
    }

    @Override
    public SupplierDto create(SupplierCreateDto dto) {
        validate(dto);
        // Unicidad
        repository.findByDocumentTypeAndIdNumber(dto.getDocumentType(), normalize(dto.getIdNumber()))
                .ifPresent(existing -> {
                    throw new DuplicateRecordException("Proveedor ya existe con ese documento");
                });

        Supplier entity = Supplier.builder()
                .name(normalize(dto.getName()))
                .documentType(dto.getDocumentType())
                .idNumber(normalize(dto.getIdNumber()))
                .phone(trimOrNull(dto.getPhone()))
                .email(trimOrNull(dto.getEmail()))
                .address(normalize(dto.getAddress()))
                .status(dto.getStatus() != null ? dto.getStatus() : SupplierStatus.ACTIVE)
                .build();

        Supplier saved = repository.save(entity);
        return mapper.mapToDto(saved);
    }

    @Override
    public SupplierDto update(String id, SupplierUpdateDto dto) {
        Supplier existing = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        validate(dto);
        // Revalidar unicidad si cambian doc/id
        DocumentType newDoc = dto.getDocumentType();
        String newIdNum = normalize(dto.getIdNumber());
        if (newDoc != null && newIdNum != null && (!newDoc.equals(existing.getDocumentType()) || !newIdNum.equals(existing.getIdNumber()))) {
            repository.findByDocumentTypeAndIdNumber(newDoc, newIdNum)
                    .ifPresent(other -> {
                        if (!other.getId().equals(existing.getId())) {
                            throw new DuplicateRecordException("Proveedor ya existe con ese documento");
                        }
                    });
        }

        existing.setName(normalize(dto.getName()));
        existing.setDocumentType(dto.getDocumentType());
        existing.setIdNumber(newIdNum);
        existing.setPhone(trimOrNull(dto.getPhone()));
        existing.setEmail(trimOrNull(dto.getEmail()));
        existing.setAddress(normalize(dto.getAddress()));
        existing.setStatus(dto.getStatus());

        Supplier saved = repository.save(existing);
        return mapper.mapToDto(saved);
    }

    @Override
    public void updateStatus(String id, SupplierStatus status) {
        if (status == null) {
            throw new FieldsException("Validation failed", Map.of("status", "Requerido"));
        }
        Supplier existing = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        existing.setStatus(status);
        repository.save(existing);
    }

    private static String normalize(String v) { return v != null ? v.trim() : null; }
    private static String trimOrNull(String v) { return (v != null && !v.trim().isEmpty()) ? v.trim() : null; }

    private void validate(SupplierCreateDto dto) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!StringUtils.hasText(dto.getName()) || dto.getName().trim().length() < 2 || dto.getName().trim().length() > 120) {
            errors.put("name", "Longitud 2..120");
        }
        if (dto.getDocumentType() == null) {
            errors.put("documentType", "Requerido");
        }
        if (!StringUtils.hasText(dto.getIdNumber()) || dto.getIdNumber().trim().length() < 3 || dto.getIdNumber().trim().length() > 30) {
            errors.put("idNumber", "Longitud 3..30");
        }
        if (!StringUtils.hasText(dto.getAddress())) {
            errors.put("address", "Requerido");
        }
        if (dto.getStatus() == null) {
            errors.put("status", "Requerido");
        }
        if (StringUtils.hasText(dto.getEmail()) && !isValidEmail(dto.getEmail().trim())) {
            errors.put("email", "Formato inválido");
        }
        if (!errors.isEmpty()) throw new FieldsException("Validation failed", errors);
    }

    private void validate(SupplierUpdateDto dto) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!StringUtils.hasText(dto.getName()) || dto.getName().trim().length() < 2 || dto.getName().trim().length() > 120) {
            errors.put("name", "Longitud 2..120");
        }
        if (dto.getDocumentType() == null) {
            errors.put("documentType", "Requerido");
        }
        if (!StringUtils.hasText(dto.getIdNumber()) || dto.getIdNumber().trim().length() < 3 || dto.getIdNumber().trim().length() > 30) {
            errors.put("idNumber", "Longitud 3..30");
        }
        if (!StringUtils.hasText(dto.getAddress())) {
            errors.put("address", "Requerido");
        }
        if (dto.getStatus() == null) {
            errors.put("status", "Requerido");
        }
        if (StringUtils.hasText(dto.getEmail()) && !isValidEmail(dto.getEmail().trim())) {
            errors.put("email", "Formato inválido");
        }
        if (!errors.isEmpty()) throw new FieldsException("Validation failed", errors);
    }

    private boolean isValidEmail(String email) {
        // Regex sencillo RFC5322-lite
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
