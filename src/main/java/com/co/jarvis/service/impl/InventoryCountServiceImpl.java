package com.co.jarvis.service.impl;

import com.co.jarvis.dto.inventorycount.*;
import com.co.jarvis.entity.*;
import com.co.jarvis.enums.InventoryCountStatus;
import com.co.jarvis.enums.UnitMeasure;
import com.co.jarvis.repository.InventoryCountSessionRepository;
import com.co.jarvis.repository.PhysicalInventoryRepository;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.service.InventoryCountService;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import com.co.jarvis.util.reports.ReportExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryCountServiceImpl implements InventoryCountService {

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InventoryCountSessionRepository sessionRepository;
    private final ProductRepository productRepository;
    private final PhysicalInventoryRepository physicalInventoryRepository;
    private final ReportExporter reportExporter;
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

        InventoryCountSession saved = sessionRepository.save(session);

        // Actualizar stock del producto inmediatamente
        Product product = productRepository.findFirstByPresentationsBarcode(request.getBarcode());
        if (product != null) {
            updateProductStock(product, List.of(request));
        }

        return saved;
    }

    @Override
    public InventoryCountSession recordBulkCount(String sessionId, BulkCountRequest request, String username) {
        log.info("InventoryCountServiceImpl -> recordBulkCount session={} entries={}", sessionId,
                request.getEntries() != null ? request.getEntries().size() : 0);
        InventoryCountSession session = getById(sessionId);

        if (session.getStatus() == InventoryCountStatus.COMPLETED ||
                session.getStatus() == InventoryCountStatus.CANCELLED) {
            throw new SaveRecordException("No se puede registrar conteo en una sesión " + session.getStatus());
        }

        for (RecordCountRequest req : request.getEntries()) {
            BigDecimal systemStock = getSystemStock(req.getBarcode());
            BigDecimal countedQty  = req.getCountedQty() != null ? req.getCountedQty() : BigDecimal.ZERO;
            BigDecimal difference  = countedQty.subtract(systemStock != null ? systemStock : BigDecimal.ZERO);

            List<InventoryCountEntry> entries = session.getEntries();
            Optional<InventoryCountEntry> existing = entries.stream()
                    .filter(e -> e.getBarcode().equals(req.getBarcode()))
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
                        .barcode(req.getBarcode())
                        .productId(req.getProductId())
                        .description(req.getDescription())
                        .presentationLabel(req.getPresentationLabel())
                        .systemStock(systemStock)
                        .countedQty(countedQty)
                        .difference(difference)
                        .countedAt(LocalDateTime.now(COLOMBIA_ZONE))
                        .countedBy(username)
                        .build());
            }
        }

        if (session.getStatus() == InventoryCountStatus.PAUSED) {
            session.setStatus(InventoryCountStatus.IN_PROGRESS);
            session.setPausedAt(null);
        }

        InventoryCountSession saved = sessionRepository.save(session);

        // Actualizar stock — todos los entries pertenecen al mismo producto
        if (request.getEntries() != null && !request.getEntries().isEmpty()) {
            Product product = productRepository.findFirstByPresentationsBarcode(
                    request.getEntries().get(0).getBarcode());
            if (product != null) {
                updateProductStock(product, request.getEntries());
            }
        }

        return saved;
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

    @Override
    public void deleteNullBarcodeRow(String productId, String physicalInventoryId) {
        log.info("InventoryCountServiceImpl -> deleteNullBarcodeRow productId={} piId={}", productId, physicalInventoryId);
        physicalInventoryRepository.deleteById(physicalInventoryId);
        productRepository.findById(productId).ifPresent(product -> {
            if (product.getPresentations() != null) {
                product.getPresentations().removeIf(p -> p.getBarcode() == null || p.getBarcode().isBlank());
                productRepository.save(product);
            }
        });
    }

    // ── Helpers de conversión de unidades ────────────────────────────────────

    /** Convierte un stock en la unidad almacenada a la unidad base de venta (ej. cm → m). */
    private double toBaseUnit(double raw, UnitMeasure um) {
        if (um == null) return raw;
        double cf = um.getConversionFactor().doubleValue();
        return cf == 1.0 ? raw : raw * cf;
    }

    /** Devuelve el nombre de la unidad base (METROS para CENTIMETROS, LITROS para MILILITROS, etc.). */
    private String baseUnitName(UnitMeasure um) {
        if (um == null) return "";
        return switch (um) {
            case CENTIMETROS -> UnitMeasure.METROS.name();
            case MILILITROS  -> UnitMeasure.LITROS.name();
            default          -> um.name();
        };
    }

    /** Indica si esta unidad requiere conversión antes de usar el costo (factor ≠ 1). */
    private boolean needsConversion(UnitMeasure um) {
        return um != null && um.getConversionFactor().compareTo(BigDecimal.ONE) != 0;
    }

    /**
     * Calcula y persiste el stock del producto basándose en las entradas de conteo.
     * stock = Σ (countedQty_i × fixedAmount_i)  —  fixedAmount_i = 1 si no aplica.
     */
    private void updateProductStock(Product product, List<RecordCountRequest> requests) {
        if (product.getStock() == null) {
            product.setStock(new Stock());
        }
        BigDecimal totalStock = BigDecimal.ZERO;
        for (RecordCountRequest req : requests) {
            BigDecimal qty    = req.getCountedQty() != null ? req.getCountedQty() : BigDecimal.ZERO;
            BigDecimal factor = BigDecimal.ONE;
            if (product.getPresentations() != null) {
                Optional<Presentation> presOpt = product.getPresentations().stream()
                        .filter(p -> req.getBarcode().equals(p.getBarcode()))
                        .findFirst();
                if (presOpt.isPresent()) {
                    Presentation pres = presOpt.get();
                    if (pres.getFixedAmount() != null
                            && pres.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                        factor = pres.getFixedAmount();
                    }
                }
            }
            totalStock = totalStock.add(qty.multiply(factor));
        }
        product.getStock().setQuantity(totalStock);
        productRepository.save(product);
        log.info("Stock actualizado para '{}': {} unidades", product.getDescription(), totalStock);
    }

    private BigDecimal getSystemStock(String barcode) {
        Product product = productRepository.findFirstByPresentationsBarcode(barcode);
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

    /**
     * Construye la etiqueta de presentación con fallback progresivo:
     * label explícito → granel → fixedAmount+unidad → unidad → barcode → "Unidad"
     */
    private String buildPresLabel(String label, Boolean isBulk, BigDecimal fixedAmount,
                                  String unitMeasureSigma, String barcode) {
        if (label != null && !label.isBlank()) return label;
        if (Boolean.TRUE.equals(isBulk)) return "Granel";
        if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ONE) > 0) {
            String fa = fixedAmount.stripTrailingZeros().toPlainString();
            return unitMeasureSigma != null && !unitMeasureSigma.isBlank()
                    ? fa + " " + unitMeasureSigma
                    : fa;
        }
        if (unitMeasureSigma != null && !unitMeasureSigma.isBlank()) return unitMeasureSigma;
        if (barcode != null && !barcode.isBlank()) return barcode;
        return "Unidad";
    }

    private String generateNumber() {
        Query query = new Query(Criteria.where("_id").is("inventory_count_session_number"));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);
        SequenceDocument doc = mongoTemplate.findAndModify(query, update, options, SequenceDocument.class, "sequences");
        if (doc == null) throw new SaveRecordException("Error al generar número de sesión de conteo");
        return "CONT-" + String.format("%04d", doc.getSeq());
    }

    @Override
    public PhysicalInventoryValueReportDto getValueReportData(PhysicalInventoryValueReportFilter filter) {
        log.info("InventoryCountServiceImpl -> getValueReportData from={} to={}", filter.getFromDate(), filter.getToDate());
        return buildValueReportDto(filter);
    }

    @Override
    public byte[] generateValuePdf(PhysicalInventoryValueReportFilter filter) throws JRException, FileNotFoundException {
        log.info("InventoryCountServiceImpl -> generateValuePdf from={} to={}", filter.getFromDate(), filter.getToDate());
        return reportExporter.exportToPdf(List.of(buildValueReportDto(filter)), "physical_inventory_value");
    }

    private PhysicalInventoryValueReportDto buildValueReportDto(PhysicalInventoryValueReportFilter filter) {
        LocalDateTime from = filter.getFromDate();
        LocalDateTime to = filter.getToDate();

        List<PhysicalInventory> inventories = physicalInventoryRepository.findByDateBetween(from, to);

        // Mantener solo el conteo más reciente por (productId + presentationBarcode)
        Map<String, PhysicalInventory> latestByKey = new LinkedHashMap<>();
        inventories.stream()
                .sorted(Comparator.comparing(PhysicalInventory::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(pi -> {
                    String key = pi.getProductId() + "|" + (pi.getPresentationBarcode() != null ? pi.getPresentationBarcode() : "");
                    latestByKey.putIfAbsent(key, pi);
                });

        // Cargar productos en bloque
        Set<String> productIds = latestByKey.values().stream()
                .map(PhysicalInventory::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // IDs de productos registrados sin presentationBarcode → se expanden por conversión
        Set<String> expandedProductIds = latestByKey.values().stream()
                .filter(pi -> pi.getPresentationBarcode() == null || pi.getPresentationBarcode().isBlank())
                .map(PhysicalInventory::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Construir filas del reporte
        List<PhysicalInventoryValueRowDto> rows = new ArrayList<>();
        for (PhysicalInventory pi : latestByKey.values()) {
            Product product = productMap.get(pi.getProductId());
            String countDate = pi.getDate() != null ? pi.getDate().format(DATE_FMT) : "";

            if ((pi.getPresentationBarcode() == null || pi.getPresentationBarcode().isBlank())
                    && product != null
                    && product.getPresentations() != null
                    && !product.getPresentations().isEmpty()) {
                // Sin barcode de presentación: distribuir el stock entre las presentaciones del catálogo
                rows.addAll(expandByPresentations(pi, product, countDate));
            } else {
                // Registro normal: una presentación específica por barcode
                rows.add(buildSingleRow(pi, product, countDate));
            }
        }

        // Separar filas sin código de barras → pestaña de limpieza
        List<PhysicalInventoryValueRowDto> nullBarcodeRows = rows.stream()
                .filter(r -> r.getBarcode() == null || r.getBarcode().isBlank())
                .collect(Collectors.toList());
        rows.removeIf(r -> r.getBarcode() == null || r.getBarcode().isBlank());

        rows.sort(Comparator.comparing(
                (PhysicalInventoryValueRowDto r) ->
                        (r.getDescription() != null ? r.getDescription() : "") + "|" +
                        (r.getPresentationLabel() != null ? r.getPresentationLabel() : ""),
                Comparator.naturalOrder()));

        BigDecimal grandTotal = rows.stream()
                .map(r -> r.getTotalValue() != null ? r.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sin costo: cruce entre barcodes contados en el período y catálogo actual.
        // Se divide en dos grupos: conteo > 0 (noCostRows) y conteo = 0 (noCostZeroRows).
        Map<String, Double> stockByBarcode = rows.stream()
                .filter(r -> r.getBarcode() != null)
                .collect(Collectors.toMap(
                        PhysicalInventoryValueRowDto::getBarcode,
                        r -> r.getPhysicalStock() != null ? r.getPhysicalStock() : 0.0,
                        Double::sum));

        List<PhysicalInventoryValueRowDto> noCostRows = new ArrayList<>();
        List<PhysicalInventoryValueRowDto> noCostZeroRows = new ArrayList<>();
        List<Product> allProducts = productRepository.findAll();
        for (Product product : allProducts) {
            if (product.getPresentations() == null) continue;
            for (Presentation pres : product.getPresentations()) {
                if (Boolean.FALSE.equals(pres.getActive())) continue;
                if (pres.getBarcode() == null || pres.getBarcode().isBlank()) continue;
                if (!stockByBarcode.containsKey(pres.getBarcode())) continue;
                if (pres.getCostPrice() != null && pres.getCostPrice().compareTo(BigDecimal.ZERO) > 0) continue;
                String sigma = pres.getUnitMeasure() != null ? pres.getUnitMeasure().getSigma() : "";
                String presLabel = buildPresLabel(pres.getLabel(), pres.getIsBulk(), pres.getFixedAmount(), sigma, pres.getBarcode());
                double stock = stockByBarcode.getOrDefault(pres.getBarcode(), 0.0);
                PhysicalInventoryValueRowDto row = PhysicalInventoryValueRowDto.builder()
                        .productId(product.getId())
                        .description(product.getDescription())
                        .presentationLabel(presLabel)
                        .barcode(pres.getBarcode())
                        .physicalStock(stock)
                        .unitMeasure(pres.getUnitMeasure() != null ? pres.getUnitMeasure().name() : "")
                        .fixedAmount(pres.getFixedAmount())
                        .unitCost(BigDecimal.ZERO)
                        .salePrice(pres.getSalePrice() != null ? pres.getSalePrice() : BigDecimal.ZERO)
                        .totalValue(BigDecimal.ZERO)
                        .build();
                if (stock > 0) {
                    noCostRows.add(row);
                } else {
                    noCostZeroRows.add(row);
                }
            }
        }
        Comparator<PhysicalInventoryValueRowDto> noCostSort = Comparator.comparing(
                (PhysicalInventoryValueRowDto r) ->
                        (r.getDescription() != null ? r.getDescription() : "") + "|" +
                        (r.getPresentationLabel() != null ? r.getPresentationLabel() : ""),
                Comparator.naturalOrder());
        noCostRows.sort(noCostSort);
        noCostZeroRows.sort(noCostSort);

        // Presentaciones no contadas: excluir las de productos expandidos (cubiertas por conversión)
        Set<String> countedKeys = latestByKey.keySet();
        List<PhysicalInventoryUncountedDto> uncountedProducts = new ArrayList<>();
        for (Product product : allProducts) {
            if (product.getPresentations() == null) continue;
            // Si el producto fue contado sin barcode ya se expandió completamente → ninguna presentación es "sin conteo"
            if (expandedProductIds.contains(product.getId())) continue;
            for (Presentation pres : product.getPresentations()) {
                if (Boolean.FALSE.equals(pres.getActive())) continue;
                String key = product.getId() + "|" + (pres.getBarcode() != null ? pres.getBarcode() : "");
                if (!countedKeys.contains(key)) {
                    uncountedProducts.add(PhysicalInventoryUncountedDto.builder()
                            .description(product.getDescription())
                            .label(pres.getLabel())
                            .barcode(pres.getBarcode())
                            .unitMeasure(pres.getUnitMeasure() != null ? pres.getUnitMeasure().name() : "")
                            .build());
                }
            }
        }
        uncountedProducts.sort(Comparator.comparing(PhysicalInventoryUncountedDto::getDescription,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return PhysicalInventoryValueReportDto.builder()
                .fromDate(from != null ? from.format(DATE_FMT) : "")
                .toDate(to != null ? to.format(DATE_FMT) : "")
                .generatedAt(LocalDateTime.now().format(DATETIME_FMT))
                .grandTotal(grandTotal)
                .rows(rows)
                .noCostRows(noCostRows)
                .noCostZeroRows(noCostZeroRows)
                .nullBarcodeRows(nullBarcodeRows)
                .uncountedProducts(uncountedProducts)
                .build();
    }

    /**
     * Para registros sin barcode de presentación: distribuye el stock total entre las presentaciones
     * activas usando greedy DESC por fixedAmount. La división se hace en la unidad almacenada
     * (ej. cm) para que fixedAmount sea comparable. El sobrante va a la última presentación
     * (granel), cuya cantidad se convierte a la unidad base (m) si aplica.
     */
    private List<PhysicalInventoryValueRowDto> expandByPresentations(PhysicalInventory pi, Product product, String countDate) {
        double rawTotal = pi.getPhysicalStock() != null ? pi.getPhysicalStock() : 0.0;
        String productName = product.getDescription();

        List<Presentation> active = product.getPresentations().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActive()))
                .sorted(Comparator.comparing(
                        p -> p.getFixedAmount() != null ? p.getFixedAmount() : BigDecimal.ONE,
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        if (active.isEmpty()) {
            return List.of(buildSingleRow(pi, product, countDate));
        }

        List<PhysicalInventoryValueRowDto> result = new ArrayList<>();
        // El greedy opera en la unidad almacenada (sin convertir) para que fixedAmount sea coherente
        double remaining = rawTotal;

        for (int i = 0; i < active.size(); i++) {
            Presentation pres = active.get(i);
            boolean isLast = (i == active.size() - 1);
            double fa = (pres.getFixedAmount() != null && pres.getFixedAmount().compareTo(BigDecimal.ZERO) > 0)
                    ? pres.getFixedAmount().doubleValue() : 1.0;

            double qty;
            if (isLast) {
                qty = remaining;
            } else {
                qty = Math.floor(remaining / fa);
                remaining = remaining - qty * fa;
            }

            if (qty <= 0) continue;

            // Para presentaciones intermedias (rollos/bultos): qty ya es el conteo de presentaciones.
            // Para la última (granel/sobrante): qty está en la unidad almacenada → convertir si aplica.
            double displayQty = qty;
            String displayUnit = pres.getUnitMeasure() != null ? pres.getUnitMeasure().name() : "";
            if (isLast && needsConversion(pres.getUnitMeasure())) {
                displayQty = toBaseUnit(qty, pres.getUnitMeasure());
                displayUnit = baseUnitName(pres.getUnitMeasure());
            }

            BigDecimal unitCost = pres.getCostPrice() != null ? pres.getCostPrice() : BigDecimal.ZERO;
            BigDecimal salePrice = pres.getSalePrice() != null ? pres.getSalePrice() : BigDecimal.ZERO;
            String unitMeasureSigma = pres.getUnitMeasure() != null ? pres.getUnitMeasure().getSigma() : "";
            String presLabel = buildPresLabel(pres.getLabel(), pres.getIsBulk(), pres.getFixedAmount(), unitMeasureSigma, pres.getBarcode());
            BigDecimal totalValue = unitCost.multiply(BigDecimal.valueOf(displayQty)).setScale(0, RoundingMode.HALF_UP);

            result.add(PhysicalInventoryValueRowDto.builder()
                    .productId(pi.getProductId())
                    .physicalInventoryId(pi.getId())
                    .description(productName)
                    .presentationLabel(presLabel)
                    .barcode(pres.getBarcode())
                    .countDate(countDate)
                    .physicalStock(displayQty)
                    .unitMeasure(displayUnit)
                    .fixedAmount(pres.getFixedAmount())
                    .unitCost(unitCost)
                    .salePrice(salePrice)
                    .totalValue(totalValue)
                    .build());
        }

        return result.isEmpty() ? List.of(buildSingleRow(pi, product, countDate)) : result;
    }

    /** Construye una sola fila a partir de un PhysicalInventory con presentationBarcode conocido. */
    private PhysicalInventoryValueRowDto buildSingleRow(PhysicalInventory pi, Product product, String countDate) {
        BigDecimal unitCost = BigDecimal.ZERO;
        BigDecimal salePrice = BigDecimal.ZERO;
        String unitMeasureStr = "";
        String unitMeasureSigma = "";
        String rawLabel = null;
        Boolean isBulk = null;
        BigDecimal fixedAmount = null;

        if (product != null && pi.getPresentationBarcode() != null && product.getPresentations() != null) {
            Optional<Presentation> presOpt = product.getPresentations().stream()
                    .filter(p -> pi.getPresentationBarcode().equals(p.getBarcode()))
                    .findFirst();
            if (presOpt.isPresent()) {
                Presentation pres = presOpt.get();
                unitCost = pres.getCostPrice() != null ? pres.getCostPrice() : BigDecimal.ZERO;
                salePrice = pres.getSalePrice() != null ? pres.getSalePrice() : BigDecimal.ZERO;
                if (pres.getUnitMeasure() != null) {
                    unitMeasureStr = pres.getUnitMeasure().name();
                    unitMeasureSigma = pres.getUnitMeasure().getSigma();
                }
                rawLabel = pres.getLabel();
                isBulk = pres.getIsBulk();
                fixedAmount = pres.getFixedAmount();
            }
        }

        String presLabel = buildPresLabel(rawLabel, isBulk, fixedAmount, unitMeasureSigma, pi.getPresentationBarcode());
        String productName = product != null
                ? product.getDescription()
                : (pi.getProduct() != null ? pi.getProduct().getDescription() : "");
        double rawStock = pi.getPhysicalStock() != null ? pi.getPhysicalStock() : 0.0;

        // Determinar cantidad a mostrar y valor total:
        // · Presentación con fixedAmount > 1 (bulto/rollo completo): mostrar conteo de presentaciones
        //   (rawStock / fixedAmount) y no convertir unidades, ya que el costo es por presentación.
        // · Granel o sin fixedAmount: si la unidad tiene factor de conversión (cm→m), convertir
        //   para mostrar en la unidad base y calcular correctamente el valor.
        double physicalStock;
        boolean hasPack = fixedAmount != null && fixedAmount.compareTo(BigDecimal.ONE) > 0
                && !Boolean.TRUE.equals(isBulk);
        if (hasPack) {
            physicalStock = rawStock / fixedAmount.doubleValue();
        } else {
            UnitMeasure um = null;
            if (product != null && pi.getPresentationBarcode() != null && product.getPresentations() != null) {
                um = product.getPresentations().stream()
                        .filter(p -> pi.getPresentationBarcode().equals(p.getBarcode()))
                        .map(Presentation::getUnitMeasure)
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
            }
            if (needsConversion(um)) {
                physicalStock = toBaseUnit(rawStock, um);
                unitMeasureStr = baseUnitName(um);
            } else {
                physicalStock = rawStock;
            }
        }

        BigDecimal totalValue = unitCost.multiply(BigDecimal.valueOf(physicalStock)).setScale(0, RoundingMode.HALF_UP);

        return PhysicalInventoryValueRowDto.builder()
                .productId(pi.getProductId())
                .physicalInventoryId(pi.getId())
                .description(productName)
                .presentationLabel(presLabel)
                .barcode(pi.getPresentationBarcode())
                .countDate(countDate)
                .physicalStock(physicalStock)
                .unitMeasure(unitMeasureStr)
                .fixedAmount(fixedAmount)
                .unitCost(unitCost)
                .salePrice(salePrice)
                .totalValue(totalValue)
                .build();
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
