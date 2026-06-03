package com.co.jarvis.service.impl;

import com.co.jarvis.dto.presale.CreatePreSaleRequest;
import com.co.jarvis.dto.presale.PreSaleFilterDto;
import com.co.jarvis.entity.PreSale;
import com.co.jarvis.entity.PreSaleItem;
import com.co.jarvis.entity.SequenceDocument;
import com.co.jarvis.enums.PreSaleStatus;
import com.co.jarvis.repository.PreSaleRepository;
import com.co.jarvis.service.PreSaleService;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PreSaleServiceImpl implements PreSaleService {

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private final PreSaleRepository preSaleRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public PreSale create(CreatePreSaleRequest request, String createdBy) {
        log.info("PreSaleServiceImpl -> create for seller: {}", request.getSellerName());
        LocalDateTime now = LocalDateTime.now(COLOMBIA_ZONE);
        PreSale preSale = PreSale.builder()
                .preSaleNumber(generateNumber())
                .status(PreSaleStatus.PENDING)
                .sellerName(request.getSellerName())
                .items(mapItems(request))
                .totalAmount(request.getTotalAmount())
                .notes(request.getNotes())
                .createdAt(now)
                .createdBy(createdBy)
                .finalizedAt(now)
                .build();
        PreSale saved = preSaleRepository.save(preSale);
        log.info("PreSale created: {}", saved.getPreSaleNumber());
        return saved;
    }

    @Override
    public PreSale findById(String id) {
        log.info("PreSaleServiceImpl -> findById: {}", id);
        return preSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preventa no encontrada con ID: " + id));
    }

    @Override
    public List<PreSale> list(PreSaleFilterDto filter) {
        log.info("PreSaleServiceImpl -> list");
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "createdAt"));

        if (filter != null && filter.getStatus() != null && !filter.getStatus().isBlank()) {
            try {
                PreSaleStatus statusEnum = PreSaleStatus.valueOf(filter.getStatus().toUpperCase());
                query.addCriteria(Criteria.where("status").is(statusEnum));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter value: {}", filter.getStatus());
            }
        }

        if (filter != null && filter.getSellerName() != null && !filter.getSellerName().isBlank()) {
            query.addCriteria(Criteria.where("sellerName").is(filter.getSellerName()));
        }

        if (filter != null && (filter.getFromDate() != null || filter.getToDate() != null)) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (filter.getFromDate() != null) {
                dateCriteria = dateCriteria.gte(filter.getFromDate().atStartOfDay());
            }
            if (filter.getToDate() != null) {
                dateCriteria = dateCriteria.lte(filter.getToDate().atTime(23, 59, 59));
            }
            query.addCriteria(dateCriteria);
        }

        return mongoTemplate.find(query, PreSale.class);
    }

    @Override
    public PreSale cancel(String id, String cancelledBy) {
        log.info("PreSaleServiceImpl -> cancel: {}", id);
        PreSale preSale = findById(id);
        if (preSale.getStatus() != PreSaleStatus.PENDING) {
            throw new SaveRecordException("Solo se pueden cancelar preventas en estado PENDIENTE");
        }
        preSale.setStatus(PreSaleStatus.CANCELLED);
        preSale.setCancelledBy(cancelledBy);
        preSale.setCancelledAt(LocalDateTime.now(COLOMBIA_ZONE));
        return preSaleRepository.save(preSale);
    }

    @Override
    public PreSale markAsBilled(String id, String billingId, String billedBy) {
        return markAsBilled(id, billingId, null, billedBy);
    }

    public PreSale markAsBilled(String id, String billingId, String billNumber, String billedBy) {
        log.info("PreSaleServiceImpl -> markAsBilled: {}, billingId: {}, billNumber: {}",
                id, billingId, billNumber);
        PreSale preSale = findById(id);
        if (preSale.getStatus() != PreSaleStatus.PENDING) {
            throw new SaveRecordException("La preventa ya fue procesada");
        }
        preSale.setStatus(PreSaleStatus.BILLED);
        preSale.setBillingId(billingId);
        preSale.setBillNumber(billNumber);   // número legible de la factura
        preSale.setBilledAt(LocalDateTime.now(COLOMBIA_ZONE));
        preSale.setBilledBy(billedBy);
        return preSaleRepository.save(preSale);
    }

    private String generateNumber() {
        Query query = new Query(Criteria.where("_id").is("pre_sale_number"));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);
        SequenceDocument doc = mongoTemplate.findAndModify(query, update, options, SequenceDocument.class, "sequences");
        if (doc == null) {
            throw new SaveRecordException("Error al generar número de preventa");
        }
        return "PRV-" + String.format("%04d", doc.getSeq());
    }

    private List<PreSaleItem> mapItems(CreatePreSaleRequest request) {
        if (request.getItems() == null) return List.of();
        return request.getItems().stream()
                .map(dto -> PreSaleItem.builder()
                        .barcode(dto.getBarcode())
                        .productId(dto.getProductId())
                        .description(dto.getDescription())
                        .saleType(dto.getSaleType())
                        .unitMeasure(dto.getUnitMeasure())
                        .presentationLabel(dto.getPresentationLabel())
                        .price(dto.getPrice())
                        .amount(dto.getAmount())
                        .isBulk(dto.isBulk())
                        .bulkInputAmount(dto.getBulkInputAmount())
                        .subTotal(dto.getSubTotal())
                        .build())
                .collect(Collectors.toList());
    }
}
