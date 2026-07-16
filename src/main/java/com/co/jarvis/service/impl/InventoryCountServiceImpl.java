package com.co.jarvis.service.impl;

import com.co.jarvis.dto.inventorycount.*;
import com.co.jarvis.entity.*;
import com.co.jarvis.enums.InventoryCountStatus;
import com.co.jarvis.repository.InventoryCountSessionRepository;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.service.InventoryCountService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryCountServiceImpl implements InventoryCountService {

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private final InventoryCountSessionRepository sessionRepository;
    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public InventoryCountSession createSession(String username) {
        log.info("InventoryCountServiceImpl -> createSession by {}", username);
        // Solo puede haber una sesión activa a la vez
        findActiveSession().ifPresent(s -> {
            throw new SaveRecordException("Ya existe una sesión activa: " + s.getSessionNumber() +
                    ". Finalízala o paúsala antes de crear una nueva.");
        });

        InventoryCountSession session = InventoryCountSession.builder()
                .sessionNumber(generateNumber())
                .status(InventoryCountStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now(COLOMBIA_ZONE))
                .startedBy(username)
                .entries(new ArrayList<>())
                .build();

        InventoryCountSession saved = sessionRepository.save(session);
        log.info("InventoryCountSession created: {}", saved.getSessionNumber());
        return saved;
    }

    @Override
    public InventoryCountSession getById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de conteo no encontrada: " + sessionId));
    }

    @Override
    public Optional<InventoryCountSession> findActiveSession() {
        return sessionRepository.findFirstByStatusOrderByStartedAtDesc(InventoryCountStatus.IN_PROGRESS)
                .or(() -> sessionRepository.findFirstByStatusOrderByStartedAtDesc(InventoryCountStatus.PAUSED));
    }

    @Override
    public InventoryCountSession recordCount(String sessionId, RecordCountRequest request, String username) {
        log.info("InventoryCountServiceImpl -> recordCount session={} barcode={}", sessionId, request.getBarcode());
        InventoryCountSession session = getById(sessionId);

        if (session.getStatus() == InventoryCountStatus.COMPLETED ||
                session.getStatus() == InventoryCountStatus.CANCELLED) {
            throw new SaveRecordException("No se puede registrar conteo en una sesión " + session.getStatus());
        }

        // Obtener stock actual del sistema
        BigDecimal systemStock = getSystemStock(request.getBarcode());

        BigDecimal countedQty = request.getCountedQty() != null
                ? request.getCountedQty()
                : BigDecimal.ZERO;

        BigDecimal difference = countedQty.subtract(systemStock != null ? systemStock : BigDecimal.ZERO);

        // Actualizar o agregar entrada
        List<InventoryCountEntry> entries = session.getEntries();
        Optional<InventoryCountEntry> existing = entries.stream()
                .filter(e -> e.getBarcode().equals(request.getBarcode()))
                .findFirst();

        if (existing.isPresent()) {
            InventoryCountEntry entry = existing.get();
            entry.setCountedQty(countedQty);
            entry.setSystemStock(systemStock);
            entry.setDifference(difference);
            entry.setCountedAt(LocalDateTime.now(COLOMBIA_ZONE));
            entry.setCountedBy(username);
        } else {
            entries.add(InventoryCountEntry.builder()
                    .barcode(request.getBarcode())
                    .productId(request.getProductId())
                    .description(request.getDescription())
                    .presentationLabel(request.getPresentationLabel())
                    .systemStock(systemStock)
                    .countedQty(countedQty)
                    .difference(difference)
                    .countedAt(LocalDateTime.now(COLOMBIA_ZONE))
                    .countedBy(username)
                    .build());
        }

        // Reactivar si estaba pausada
        if (session.getStatus() == InventoryCountStatus.PAUSED) {
            session.setStatus(InventoryCountStatus.IN_PROGRESS);
            session.setPausedAt(null);
        }

        return sessionRepository.save(session);
    }

    @Override
    public InventoryCountSession pauseSession(String sessionId) {
        log.info("InventoryCountServiceImpl -> pauseSession: {}", sessionId);
        InventoryCountSession session = getById(sessionId);
        if (session.getStatus() != InventoryCountStatus.IN_PROGRESS) {
            throw new SaveRecordException("Solo se puede pausar una sesión en progreso");
        }
        session.setStatus(InventoryCountStatus.PAUSED);
        session.setPausedAt(LocalDateTime.now(COLOMBIA_ZONE));
        return sessionRepository.save(session);
    }

    @Override
    public InventoryCountSession completeSession(String sessionId, String username) {
        log.info("InventoryCountServiceImpl -> completeSession: {}", sessionId);
        InventoryCountSession session = getById(sessionId);
        if (session.getStatus() == InventoryCountStatus.COMPLETED) {
            throw new SaveRecordException("La sesión ya fue completada");
        }
        if (session.getStatus() == InventoryCountStatus.CANCELLED) {
            throw new SaveRecordException("La sesión fue cancelada");
        }
        session.setStatus(InventoryCountStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now(COLOMBIA_ZONE));
        session.setCompletedBy(username);
        return sessionRepository.save(session);
    }

    @Override
    public InventoryCountReportDto getReport(String sessionId) {
        log.info("InventoryCountServiceImpl -> getReport: {}", sessionId);
        InventoryCountSession session = getById(sessionId);

        Set<String> countedBarcodes = session.getEntries().stream()
                .map(InventoryCountEntry::getBarcode)
                .collect(Collectors.toSet());

        // Construir lista de no contados
        List<UncountedProductDto> uncounted = new ArrayList<>();
        List<Product> allProducts = productRepository.findAll();

        for (Product product : allProducts) {
            if (product.getPresentations() == null) continue;
            for (Presentation pres : product.getPresentations()) {
                if (pres.getBarcode() == null || countedBarcodes.contains(pres.getBarcode())) continue;
                BigDecimal sysStock = getSystemStockFromProduct(product);
                uncounted.add(UncountedProductDto.builder()
                        .barcode(pres.getBarcode())
                        .productId(product.getId())
                        .presentationId(pres.getId())
                        .description(buildDescription(product.getDescription(), pres.getLabel()))
                        .presentationLabel(pres.getLabel())
                        .active(pres.getActive())
                        .systemStock(sysStock)
                        .build());
            }
        }

        List<InventoryCountEntryDto> countedDtos = session.getEntries().stream()
                .map(this::toEntryDto)
                .collect(Collectors.toList());

        int total = countedDtos.size() + uncounted.size();
        double coverage = total > 0
                ? BigDecimal.valueOf((double) countedDtos.size() / total * 100)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return InventoryCountReportDto.builder()
                .session(toSessionDto(session))
                .counted(countedDtos)
                .uncounted(uncounted)
                .totalPresentations(total)
                .totalCounted(countedDtos.size())
                .totalUncounted(uncounted.size())
                .coveragePercent(coverage)
                .build();
    }

    @Override
    public List<InventoryCountSessionDto> listSessions(LocalDate fromDate, LocalDate toDate) {
        log.info("InventoryCountServiceImpl -> listSessions from={} to={}", fromDate, toDate);
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "startedAt"));

        if (fromDate != null || toDate != null) {
            Criteria dateCriteria = Criteria.where("startedAt");
            if (fromDate != null) dateCriteria = dateCriteria.gte(fromDate.atStartOfDay());
            if (toDate != null) dateCriteria = dateCriteria.lte(toDate.atTime(23, 59, 59));
            query.addCriteria(dateCriteria);
        }

        return mongoTemplate.find(query, InventoryCountSession.class)
                .stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());
    }

    @Override
    public HideUncountedResultDto hideUncountedPresentations(String sessionId) {
        log.info("InventoryCountServiceImpl -> hideUncountedPresentations: {}", sessionId);
        InventoryCountSession session = getById(sessionId);

        // Barcodes que SÍ fueron contados en esta sesión
        Set<String> countedBarcodes = session.getEntries().stream()
                .map(InventoryCountEntry::getBarcode)
                .collect(Collectors.toSet());

        int hiddenCount = 0;
        List<Product> allProducts = productRepository.findAll();

        for (Product product : allProducts) {
            if (product.getPresentations() == null) continue;
            boolean modified = false;
            for (Presentation pres : product.getPresentations()) {
                if (pres.getBarcode() == null) continue;
                // Solo ocultar las que NO fueron contadas y están actualmente activas
                if (!countedBarcodes.contains(pres.getBarcode())
                        && !Boolean.FALSE.equals(pres.getActive())) {
                    pres.setActive(false);
                    modified = true;
                    hiddenCount++;
                }
            }
            if (modified) {
                productRepository.save(product);
            }
        }

        log.info("hideUncountedPresentations: {} presentaciones ocultadas para sesión {}", hiddenCount, sessionId);
        return HideUncountedResultDto.builder()
                .hidden(hiddenCount)
                .message(hiddenCount == 0
                        ? "No hay presentaciones nuevas para ocultar."
                        : hiddenCount + " presentación(es) marcadas como inactivas.")
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal getSystemStock(String barcode) {
        Product product = productRepository.findByPresentationsBarcode(barcode);
        if (product == null || product.getStock() == null) return BigDecimal.ZERO;
        return product.getStock().getQuantity() != null ? product.getStock().getQuantity() : BigDecimal.ZERO;
    }

    private BigDecimal getSystemStockFromProduct(Product product) {
        if (product.getStock() == null) return BigDecimal.ZERO;
        return product.getStock().getQuantity() != null ? product.getStock().getQuantity() : BigDecimal.ZERO;
    }

    private String buildDescription(String productDesc, String label) {
        if (label != null && !label.isBlank()) return productDesc + " - " + label;
        return productDesc;
    }

    private String generateNumber() {
        Query query = new Query(Criteria.where("_id").is("inventory_count_session_number"));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);
        SequenceDocument doc = mongoTemplate.findAndModify(query, update, options, SequenceDocument.class, "sequences");
        if (doc == null) throw new SaveRecordException("Error al generar número de sesión de conteo");
        return "CONT-" + String.format("%04d", doc.getSeq());
    }

    public InventoryCountSessionDto toSessionDto(InventoryCountSession s) {
        return InventoryCountSessionDto.builder()
                .id(s.getId())
                .sessionNumber(s.getSessionNumber())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .notes(s.getNotes())
                .startedAt(s.getStartedAt())
                .startedBy(s.getStartedBy())
                .pausedAt(s.getPausedAt())
                .completedAt(s.getCompletedAt())
                .completedBy(s.getCompletedBy())
                .entries(s.getEntries().stream().map(this::toEntryDto).collect(Collectors.toList()))
                .totalCounted(s.getEntries().size())
                .build();
    }

    private InventoryCountEntryDto toEntryDto(InventoryCountEntry e) {
        return InventoryCountEntryDto.builder()
                .barcode(e.getBarcode())
                .productId(e.getProductId())
                .description(e.getDescription())
                .presentationLabel(e.getPresentationLabel())
                .systemStock(e.getSystemStock())
                .countedQty(e.getCountedQty())
                .difference(e.getDifference())
                .countedAt(e.getCountedAt())
                .countedBy(e.getCountedBy())
                .build();
    }
}
