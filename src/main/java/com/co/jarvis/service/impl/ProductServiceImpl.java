package com.co.jarvis.service.impl;

import com.co.jarvis.dto.BulkPresentationPriceUpdateRequest;
import com.co.jarvis.dto.BulkPresentationPriceUpdateResponse;
import com.co.jarvis.dto.BulkUpdateError;
import com.co.jarvis.dto.DisplayStock;
import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.PresentationPriceUpdate;
import com.co.jarvis.dto.ProductDto;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.entity.AuditEntry;
import com.co.jarvis.entity.Presentation;
import com.co.jarvis.entity.Product;
import com.co.jarvis.enums.EAuditAction;
import com.co.jarvis.enums.ESale;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.service.ProductService;
import com.co.jarvis.util.DateTimeUtil;
import com.co.jarvis.util.UnitConverter;
import com.co.jarvis.util.exception.DeleteRecordException;
import com.co.jarvis.util.exception.DuplicateRecordException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mappers.PaginationMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private static final int DEFAULT_BARCODE_START = 1000;

    @Autowired
    private ProductRepository repository;

    GenericMapper<Product, ProductDto> mapper = new GenericMapper<>(Product.class, ProductDto.class);
    PaginationMapper<Product, ProductDto> paginationMapper = new PaginationMapper<>(Product.class, ProductDto.class);

    @Override
    public PaginationDto<ProductDto> findAllPage(int pageNumber, int pageSize) {
        log.info("ProductServiceImpl -> findAllPage");
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Product> pageProduct = repository.findAll(pageable);
        PaginationDto<ProductDto> pagination = paginationMapper.pageToPagination(pageProduct);
        
        // Enriquecer cada producto con displayStock
        if (pagination.getContent() != null) {
            pagination.getContent().forEach(dto -> {
                Product product = mapper.mapToEntity(dto);
                dto.setDisplayStock(computeDisplayStock(product));
            });
        }
        
        return pagination;
    }

    @Override
    public PaginationDto<ProductDto> findAllPageSearch(int pageNumber, int pageSize, String search) {
        log.info("ProductServiceImpl -> findAllPageSearch");
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Product> pageProduct = repository.findByPresentationsBarcodeOrDescriptionContainingIgnoreCase(search, search, pageable);
        PaginationDto<ProductDto> pagination = paginationMapper.pageToPagination(pageProduct);
        
        // Enriquecer cada producto con displayStock
        if (pagination.getContent() != null) {
            pagination.getContent().forEach(dto -> {
                Product product = mapper.mapToEntity(dto);
                dto.setDisplayStock(computeDisplayStock(product));
            });
        }
        
        return pagination;
    }

    @Override
    public BulkPresentationPriceUpdateResponse bulkUpdatePresentationPrices(
            BulkPresentationPriceUpdateRequest request, UserDto user) {
        log.info("ProductServiceImpl -> bulkUpdatePresentationPrices: entries={}, user={}",
                request != null && request.getUpdates() != null ? request.getUpdates().size() : 0,
                user != null ? user.getNumberIdentity() : "unknown");

        List<PresentationPriceUpdate> updates = request.getUpdates();
        List<BulkUpdateError> errors = new ArrayList<>();
        int[] updatedCount = {0};

        // Agrupar por productId para procesar cada producto de forma atómica
        Map<String, List<PresentationPriceUpdate>> grouped = updates.stream()
                .collect(Collectors.groupingBy(PresentationPriceUpdate::getProductId,
                        LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<PresentationPriceUpdate>> entry : grouped.entrySet()) {
            String productId = entry.getKey();
            List<PresentationPriceUpdate> productUpdates = entry.getValue();
            try {
                int applied = applyUpdatesToProduct(productId, productUpdates, user, errors);
                updatedCount[0] += applied;
            } catch (RuntimeException ex) {
                log.error("Error al actualizar producto {}: {}", productId, ex.getMessage(), ex);
                for (PresentationPriceUpdate u : productUpdates) {
                    errors.add(BulkUpdateError.builder()
                            .productId(u.getProductId())
                            .barcode(u.getBarcode())
                            .message("Error al actualizar el producto: " + ex.getMessage())
                            .build());
                }
            }
        }

        return BulkPresentationPriceUpdateResponse.builder()
                .updated(updatedCount[0])
                .failed(errors.size())
                .errors(errors)
                .build();
    }

    /**
     * Aplica todas las actualizaciones de precios para un único producto de forma atómica.
     * Si el producto no existe, cada entrada se marca con error (devuelve 0).
     * Si una presentación no existe, solo esa entrada se marca con error; las demás se aplican.
     */
    @Transactional
    protected int applyUpdatesToProduct(String productId,
                                        List<PresentationPriceUpdate> productUpdates,
                                        UserDto user,
                                        List<BulkUpdateError> errors) {
        Optional<Product> maybeProduct = repository.findById(productId);
        if (maybeProduct.isEmpty()) {
            for (PresentationPriceUpdate u : productUpdates) {
                errors.add(BulkUpdateError.builder()
                        .productId(u.getProductId())
                        .barcode(u.getBarcode())
                        .message("Producto no existe con ID: " + productId)
                        .build());
            }
            return 0;
        }

        Product product = maybeProduct.get();
        if (product.getPresentations() == null || product.getPresentations().isEmpty()) {
            for (PresentationPriceUpdate u : productUpdates) {
                errors.add(BulkUpdateError.builder()
                        .productId(u.getProductId())
                        .barcode(u.getBarcode())
                        .message("El producto no tiene presentaciones registradas")
                        .build());
            }
            return 0;
        }

        int appliedHere = 0;
        for (PresentationPriceUpdate update : productUpdates) {
            Optional<Presentation> maybePresentation = product.getPresentations().stream()
                    .filter(p -> Objects.equals(p.getBarcode(), update.getBarcode()))
                    .findFirst();

            if (maybePresentation.isEmpty()) {
                errors.add(BulkUpdateError.builder()
                        .productId(update.getProductId())
                        .barcode(update.getBarcode())
                        .message("Barcode no existe en las presentaciones del producto")
                        .build());
                continue;
            }

            Presentation presentation = maybePresentation.get();
            BigDecimal oldSalePrice = presentation.getSalePrice();
            BigDecimal oldCostPrice = presentation.getCostPrice();

            boolean changedSomething = false;

            if (update.getSalePrice() != null
                    && !Objects.equals(oldSalePrice, update.getSalePrice())) {
                presentation.setSalePrice(update.getSalePrice());
                appendAuditEntry(product, user, EAuditAction.ACTUALIZACION_PRECIO_VENTA,
                        update.getBarcode(), "salePrice", oldSalePrice, update.getSalePrice());
                changedSomething = true;
            }
            if (update.getCostPrice() != null
                    && !Objects.equals(oldCostPrice, update.getCostPrice())) {
                presentation.setCostPrice(update.getCostPrice());
                appendAuditEntry(product, user, EAuditAction.ACTUALIZACION_PRECIO_COSTO,
                        update.getBarcode(), "costPrice", oldCostPrice, update.getCostPrice());
                changedSomething = true;
            }

            // Advertencia no bloqueante: precio de venta menor al costo
            BigDecimal effectiveSale = presentation.getSalePrice();
            BigDecimal effectiveCost = presentation.getCostPrice();
            if (effectiveSale != null && effectiveCost != null
                    && effectiveSale.compareTo(effectiveCost) < 0) {
                log.warn("ADVERTENCIA: salePrice ({}) < costPrice ({}) en product={} barcode={}",
                        effectiveSale, effectiveCost, productId, update.getBarcode());
            }

            logAuditEntry(productId, update.getBarcode(), oldSalePrice, update.getSalePrice(),
                    oldCostPrice, update.getCostPrice(), user);
            if (changedSomething) {
                appliedHere++;
            }
        }

        if (appliedHere > 0) {
            repository.save(product);
        }
        return appliedHere;
    }

    private void appendAuditEntry(Product product, UserDto user, EAuditAction action,
                                  String barcode, String fieldName,
                                  BigDecimal oldValue, BigDecimal newValue) {
        if (product.getAuditTrail() == null) {
            product.setAuditTrail(new ArrayList<>());
        }
        AuditEntry entry = AuditEntry.builder()
                .userId(user != null ? user.getNumberIdentity() : null)
                .userName(user != null ? user.getFullName() : null)
                .action(action)
                .timestamp(DateTimeUtil.nowLocalDateTime())
                .entityRef("presentation:" + barcode)
                .fieldName(fieldName)
                .oldValue(oldValue != null ? oldValue.toPlainString() : null)
                .newValue(newValue != null ? newValue.toPlainString() : null)
                .details(String.format("Actualización de %s en presentación %s: %s -> %s",
                        fieldName, barcode,
                        oldValue != null ? oldValue.toPlainString() : "null",
                        newValue != null ? newValue.toPlainString() : "null"))
                .build();
        product.getAuditTrail().add(entry);
    }

    private void logAuditEntry(String productId, String barcode,
                               BigDecimal oldSalePrice, BigDecimal newSalePrice,
                               BigDecimal oldCostPrice, BigDecimal newCostPrice,
                               UserDto user) {
        String userId = user != null ? user.getNumberIdentity() : "unknown";
        String userName = user != null ? user.getFullName() : "unknown";
        log.info("AUDIT PRICE_UPDATE | user={} ({}) | product={} | barcode={} | " +
                        "salePrice: {} -> {} | costPrice: {} -> {} | at={}",
                userName, userId, productId, barcode,
                oldSalePrice, newSalePrice != null ? newSalePrice : "(sin cambio)",
                oldCostPrice, newCostPrice != null ? newCostPrice : "(sin cambio)",
                Instant.now());
    }

    @Override
    public ProductDto findByPresentationsBarcode(String barcode) {
        Product product = repository.findByPresentationsBarcode(barcode);
        return product != null ? enrichProductDto(product) : null;
    }

    @Override
    public void decreaseStock(Product product, BigDecimal amount) {
        if (product == null) {
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        }
        product.reduceStock(amount);
        repository.save(product);
    }

    @Override
    public void increaseStock(Product product, BigDecimal amount) {
        if (product == null) {
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        }
        product.increaseStock(amount);
        repository.save(product);
    }

    @Override
    public String validateOrGenerateBarcode(String barcode) {
        // Validación y generación robusta del código de barras
        try {
            if (barcode != null && !barcode.isBlank()) {
                String trimmed = barcode.trim();

                // Validación de formato para códigos internos: 4 dígitos numéricos
                // Los códigos externos (EAN-13, UPC, etc.) pueden tener más de 4 dígitos
                if (trimmed.length() <= 4 && !trimmed.matches("\\d{4}")) {
                    throw new SaveRecordException("El código de barras interno debe ser numérico de 4 dígitos.");
                }

                // Verificar unicidad: si ya existe, lanzar excepción
                ProductDto existing = findByPresentationsBarcode(trimmed);
                if (existing != null) {
                    Presentation presentation = existing.getPresentations().stream()
                            .filter(p -> Objects.equals(trimmed, p.getBarcode()))
                            .findFirst()
                            .orElse(null);
                    String presentLabel = presentation != null ? presentation.getLabel() : "N/A";
                    throw new DuplicateRecordException(format(
                            "El código de barras %s ya existe. Está asignado al producto: %s en la presentación: %s",
                            trimmed, existing.getDescription(), presentLabel));
                }

                // Si pasó todas las validaciones y no existe, retornarlo
                return trimmed;
            }

            // Si no se envía barcode, generar el siguiente automáticamente basado en el último de la BD
            String lastBarcode = repository.findHighestBarcodeAsString();
            int nextNumber = DEFAULT_BARCODE_START;
            
            if (lastBarcode != null && !lastBarcode.isEmpty() && lastBarcode.matches("\\d{4}")) {
                try {
                    int currentNumber = Integer.parseInt(lastBarcode);
                    nextNumber = currentNumber + 1;
                } catch (NumberFormatException e) {
                    logger.warn("El último barcode no es numérico válido: {}", lastBarcode);
                }
            }

            return format("%04d", nextNumber);
        } catch (DuplicateRecordException | SaveRecordException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error en validateOrGenerateBarcode: {}", e.getMessage(), e);
            throw new SaveRecordException("Error validando/generando el código de barras.");
        }
    }

    @Override
    public String generateNextProductCode() {
        Product product = repository.findTopByOrderByProductCodeDesc();
        String nextCode = "P001"; // Valor inicial si no existe ninguno

        if (product != null && product.getProductCode() != null) {
            try {
                String currentNumber = product.getProductCode().substring(1); // Elimina la 'P'
                int nextNumber = Integer.parseInt(currentNumber) + 1;
                nextCode = String.format("P%03d", nextNumber); // Formato P seguido de 3 dígitos
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                logger.warn("El código de producto no tiene el formato correcto: {}", product.getProductCode());
            }
        }
        return nextCode;
    }


    @Override
    public List<ProductDto> findAll() {
        log.info("ProductServiceImpl -> findAll");
        List<Product> products = repository.findAll();
        return products.stream()
            .map(this::enrichProductDto)
            .toList();
    }

    @Override
    public ProductDto findById(String id) {
        Optional<Product> producto = repository.findById(id);
        return producto.map(this::enrichProductDto).orElse(null);
    }

    @Override
    public ProductDto save(ProductDto dto) {
        log.info("ProductServiceImpl -> save");
        try {
            // Si el producto ya existe (tiene id), redirigir a updatePresent para proteger campos estructurales
            if (dto.getId() != null && repository.existsById(dto.getId())) {
                log.info("Producto con id {} ya existe, redirigiendo a updatePresent", dto.getId());
                return updatePresent(dto, dto.getId());
            }

            Product product = mapper.mapToEntity(dto);
            product = repository.save(product);
            return enrichProductDto(product);
        } catch (DuplicateRecordException e) {
            log.error("ProductServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            log.error("ProductServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR);
        }
    }

    @Override
    public void deleteById(String id) {
        log.info("ProductServiceImpl -> deleteById");
        try {
            repository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            log.error("ProductServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("ProductServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new DeleteRecordException(MessageConstants.DELETE_RECORD_ERROR);
        }
    }

    @Override
    public ProductDto update(ProductDto dto, String barcode) {
        log.info("ProductServiceImpl -> update");
        try {
            Product product = Optional.ofNullable(repository.findByPresentationsBarcode(barcode)).orElseThrow(()
                    -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));

            return updatePresent(dto, product.getId());
        } catch (ResourceNotFoundException e) {
            log.error("ProductServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND, e);
        } catch (DuplicateRecordException e) {
            log.error("ProductServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            log.error("ProductServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e);
        }
    }

    private ProductDto updatePresent(ProductDto dto, String id) {
        log.info("ProductServiceImpl -> updatePresent");

        // Cargar producto existente de la DB (fuente de verdad)
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));

        // Actualizar solo campos generales que el frontend puede modificar
        product.setDescription(dto.getDescription());
        product.setSaleType(dto.getSaleType());
        product.setBrand(dto.getBrand());
        product.setCategory(dto.getCategory());
        product.setProductCode(dto.getProductCode());
        product.setVatValue(dto.getVatValue());
        product.setVatType(dto.getVatType());

        if (dto.getPresentations() != null && product.getPresentations() != null) {
            for (Presentation dtoPres : dto.getPresentations()) {
                if (dtoPres.getBarcode() == null) continue;
                product.getPresentations().stream()
                        .filter(dbPres -> dtoPres.getBarcode().equals(dbPres.getBarcode()))
                        .findFirst()
                        .ifPresent(dbPres -> {
                            dbPres.setSalePrice(dtoPres.getSalePrice());
                            dbPres.setCostPrice(dtoPres.getCostPrice());
                            dbPres.setLabel(dtoPres.getLabel());
                            dbPres.setUnitMeasure(dtoPres.getUnitMeasure());
                            dbPres.setProductCode(dtoPres.getProductCode());
                            dbPres.setFixedAmount(dtoPres.getFixedAmount());
                            dbPres.setIsFixedAmount(dtoPres.getIsFixedAmount());
                            dbPres.setIsBulk(dtoPres.getIsBulk());
                            
                            log.info("Presentación {} actualizada: salePrice={}, costPrice={}, label={} | fixedAmount={} (protegido)",
                                    dbPres.getBarcode(), dbPres.getSalePrice(), dbPres.getCostPrice(),
                                    dbPres.getLabel(), dbPres.getFixedAmount());
                        });
            }

            // Agregar presentaciones nuevas (las que no existen en la DB)
            for (Presentation dtoPres : dto.getPresentations()) {
                if (dtoPres.getBarcode() == null) continue;
                boolean existsInDb = product.getPresentations().stream()
                        .anyMatch(dbPres -> dtoPres.getBarcode().equals(dbPres.getBarcode()));
                if (!existsInDb) {
                    product.getPresentations().add(dtoPres);
                    log.info("Nueva presentación agregada: barcode={}, fixedAmount={}, isFixedAmount={}, isBulk={}",
                            dtoPres.getBarcode(), dtoPres.getFixedAmount(), dtoPres.getIsFixedAmount(), dtoPres.getIsBulk());
                }
            }
        }

        product = repository.save(product);
        log.info("Producto {} actualizado. Presentaciones guardadas:", product.getProductCode());
        if (product.getPresentations() != null) {
            product.getPresentations().forEach(p ->
                    log.info("  -> barcode={}, fixedAmount={}, isFixedAmount={}, isBulk={}",
                            p.getBarcode(), p.getFixedAmount(), p.getIsFixedAmount(), p.getIsBulk()));
        }
        return enrichProductDto(product);
    }

    /**
     * Enriquece un ProductDto con el displayStock calculado
     */
    private ProductDto enrichProductDto(Product product) {
        ProductDto dto = mapper.mapToDto(product);
        dto.setDisplayStock(computeDisplayStock(product));
        return dto;
    }

    @Override
    public DisplayStock computeDisplayStock(Product product) {
        log.info("ProductServiceImpl -> computeDisplayStock");
        
        if (product == null || product.getStock() == null) {
            return buildEmptyDisplayStock();
        }

        ESale saleType = product.getSaleType();
        String kind = determineKind(saleType);
        String unitBase = determineBaseUnit(kind, product.getStock().getUnitMeasure());
        
        // Convertir cantidad de stock a unidad base
        BigDecimal qty = UnitConverter.toBase(
            product.getStock().getQuantity() != null ? product.getStock().getQuantity() : BigDecimal.ZERO,
            product.getStock().getUnitMeasure(),
            unitBase
        );
        
        // Si qty es negativo o cero, tratarlo como 0
        if (qty.compareTo(BigDecimal.ZERO) < 0) {
            qty = BigDecimal.ZERO;
        }

        // Si no es WEIGHT ni LONGITUDE, retornar stock simple
        if (kind == null) {
            return DisplayStock.builder()
                .kind(null)
                .packSize(null)
                .packs(null)
                .remainder(null)
                .unit(unitBase)
                .label(format("%s %s", qty.stripTrailingZeros().toPlainString(), unitBase))
                .computedAt(Instant.now().toString())
                .build();
        }

        // Buscar el packSize (mayor fixedAmount de presentaciones con isFixedAmount=true)
        BigDecimal packSize = findLargestPackSize(product, unitBase);
        
        if (packSize == null || packSize.compareTo(BigDecimal.ZERO) <= 0) {
            // No hay presentaciones fijas válidas
            return DisplayStock.builder()
                .kind(kind)
                .packSize(null)
                .packs(null)
                .remainder(null)
                .unit(unitBase)
                .label(format("%s %s", qty.stripTrailingZeros().toPlainString(), unitBase))
                .computedAt(Instant.now().toString())
                .build();
        }

        // Calcular packs y remainder
        int packs = qty.divide(packSize, 0, RoundingMode.DOWN).intValue();
        BigDecimal remainder = qty.subtract(packSize.multiply(new BigDecimal(packs)));
        remainder = UnitConverter.round(remainder, 3);

        // Construir label
        String noun = "WEIGHT".equals(kind) ? "bultos" : "rollos";
        String label = format("%d %s + %s %s", 
            packs, 
            noun, 
            remainder.stripTrailingZeros().toPlainString(), 
            unitBase
        );

        return DisplayStock.builder()
            .kind(kind)
            .packSize(packSize)
            .packs(packs)
            .remainder(remainder)
            .unit(unitBase)
            .label(label)
            .computedAt(Instant.now().toString())
            .build();
    }

    private String determineKind(ESale saleType) {
        if (saleType == null) {
            return null;
        }
        return switch (saleType) {
            case WEIGHT -> "WEIGHT";
            case LONGITUDE -> "LONGITUDE";
            default -> null;
        };
    }

    private String determineBaseUnit(String kind, com.co.jarvis.enums.UnitMeasure stockUnit) {
        if (kind == null) {
            return stockUnit != null ? stockUnit.getSigma() : "";
        }
        return switch (kind) {
            case "WEIGHT" -> "kg";
            case "LONGITUDE" -> "cm";
            default -> stockUnit != null ? stockUnit.getSigma() : "";
        };
    }

    private BigDecimal findLargestPackSize(Product product, String unitBase) {
        if (product.getPresentations() == null || product.getPresentations().isEmpty()) {
            return null;
        }

        return product.getPresentations().stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsFixedAmount()))
            .filter(p -> p.getFixedAmount() != null && p.getFixedAmount().compareTo(BigDecimal.ZERO) > 0)
            .map(p -> UnitConverter.toBase(p.getFixedAmount(), p.getUnitMeasure(), unitBase))
            .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    private DisplayStock buildEmptyDisplayStock() {
        return DisplayStock.builder()
            .kind(null)
            .packSize(null)
            .packs(null)
            .remainder(null)
            .unit("")
            .label("0")
            .computedAt(Instant.now().toString())
            .build();
    }
}
