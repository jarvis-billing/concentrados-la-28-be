package com.co.jarvis.service.impl;

import com.co.jarvis.dto.SupplierPaymentDto;
import com.co.jarvis.entity.SupplierPayment;
import com.co.jarvis.enums.EPaymentMethod;
import com.co.jarvis.repository.BankAccountRepository;
import com.co.jarvis.repository.SupplierPaymentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.co.jarvis.service.SupplierPaymentService;
import com.co.jarvis.util.exception.FieldsException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.mappers.GenericMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private static final long MAX_FILE_BYTES = 5L * 1024L * 1024L; // 5 MB
    private static final String STORAGE_DIR = "uploads/supplier-payments";

    @Autowired
    private SupplierPaymentRepository repository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final GenericMapper<SupplierPayment, SupplierPaymentDto> mapper =
            new GenericMapper<>(SupplierPayment.class, SupplierPaymentDto.class);

    @Override
    public SupplierPaymentDto create(SupplierPaymentDto dto, MultipartFile support) throws IOException {
        validate(dto, support);

        SupplierPayment entity = mapper.mapToEntity(dto);
        entity.setId(UUID.randomUUID().toString());

        // Campos de vinculación — inicializar siempre en alta
        entity.setStatus("ADELANTO");
        entity.setAppliedAmount(BigDecimal.ZERO);
        entity.setRemainingAmount(entity.getAmount());
        entity.setLinkedPurchaseId(null);
        entity.setLinkedAt(null);
        entity.setLinkedBy(null);
        entity.setCreatedAt(java.time.OffsetDateTime.now());
        entity.setUpdatedAt(java.time.OffsetDateTime.now());

        // Enriquecer nombre de cuenta bancaria si viene solo el ID
        if (StringUtils.hasText(entity.getBankAccountId()) && !StringUtils.hasText(entity.getBankAccountName())) {
            bankAccountRepository.findById(entity.getBankAccountId())
                .ifPresent(ba -> entity.setBankAccountName(ba.getBankName()));
        }

        if (support != null && !support.isEmpty()) {
            String filename = buildFilename(entity.getId(), support.getOriginalFilename());
            Path dir = Paths.get(STORAGE_DIR);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.copy(support.getInputStream(), filePath);
            entity.setSupportPath(filePath.toAbsolutePath().toString());
            entity.setSupportUrl("/api/supplier-payments/" + entity.getId() + "/support");
        }

        SupplierPayment saved = repository.save(entity);
        return mapper.mapToDto(saved);
    }

    @Override
    public List<SupplierPaymentDto> list(String supplierId, LocalDate from, LocalDate to,
                                         String status, Boolean unlinkedOnly, String bankAccountId) {
        Query query = new Query();

        if (isValidParam(supplierId)) {
            query.addCriteria(Criteria.where("supplierId").is(supplierId));
        }
        if (from != null && to != null) {
            LocalDate adjustedFrom = from.equals(to) ? from : from.minusDays(1);
            LocalDate adjustedTo = from.equals(to) ? to : to.plusDays(1);
            query.addCriteria(Criteria.where("paymentDate").gte(adjustedFrom).lte(adjustedTo));
        }
        if (StringUtils.hasText(status)) {
            query.addCriteria(Criteria.where("status").is(status));
        } else if (Boolean.TRUE.equals(unlinkedOnly)) {
            query.addCriteria(Criteria.where("status").in("ADELANTO", "PARCIAL"));
        }
        if (isValidParam(bankAccountId)) {
            query.addCriteria(Criteria.where("bankAccountId").is(bankAccountId));
        }
        query.with(Sort.by(Sort.Direction.DESC, "paymentDate"));

        List<SupplierPayment> result = mongoTemplate.find(query, SupplierPayment.class);
        return mapper.mapToDtoList(result);
    }

    @Override
    public byte[] getSupport(String id) throws IOException {
        SupplierPayment sp = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found"));
        if (!StringUtils.hasText(sp.getSupportPath())) {
            throw new ResourceNotFoundException("Support file not found");
        }
        return Files.readAllBytes(Paths.get(sp.getSupportPath()));
    }

    @Override
    public String getSupportContentType(String id) throws IOException {
        SupplierPayment sp = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found"));
        if (!StringUtils.hasText(sp.getSupportPath())) {
            throw new ResourceNotFoundException("Support file not found");
        }
        return Files.probeContentType(Paths.get(sp.getSupportPath()));
    }

    private boolean isValidParam(String value) {
        return StringUtils.hasText(value) && !"undefined".equalsIgnoreCase(value);
    }

    private static String buildFilename(String id, String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return id + ext;
    }

    private void validate(SupplierPaymentDto dto, MultipartFile support) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!StringUtils.hasText(dto.getSupplierId())) {
            errors.put("supplierId", "Requerido");
        }
        if (dto.getPaymentDate() == null) {
            errors.put("paymentDate", "Requerido (yyyy-MM-dd)");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.put("amount", "Debe ser mayor a 0");
        }
        if (dto.getMethod() == null) {
            errors.put("method", "Requerido");
        } else {
            try {
                EPaymentMethod.valueOf(dto.getMethod().name());
            } catch (IllegalArgumentException ex) {
                errors.put("method", "Método inválido");
            }
            // Si es transferencia, requerir cuenta bancaria
            if (dto.getMethod() == EPaymentMethod.TRANSFERENCIA && !StringUtils.hasText(dto.getBankAccountId())) {
                errors.put("bankAccountId", "Requerido para pagos por transferencia");
            }
        }

        if (support != null && !support.isEmpty()) {
            if (support.getSize() > MAX_FILE_BYTES) {
                errors.put("support", "Archivo supera 5MB");
            }
            String contentType = support.getContentType();
            if (contentType == null || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
                errors.put("support", "Tipo de archivo no permitido (image/*, application/pdf)");
            }
        }

        if (!errors.isEmpty()) {
            throw new FieldsException("Validation failed", errors);
        }
    }
}
