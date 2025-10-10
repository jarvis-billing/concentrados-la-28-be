package com.co.jarvis.service.impl;

import com.co.jarvis.dto.DisplayStock;
import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.ProductDto;
import com.co.jarvis.entity.Presentation;
import com.co.jarvis.entity.Product;
import com.co.jarvis.enums.ESale;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.service.ProductService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Value("${barcode.start.digit}")
    private String barcodeStartDigit;

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
    public void updatePriceByIds(BigDecimal price, List<String> ids) {
        log.info("ProductServiceImpl -> updatePriceByIds");
        List<Product> products = repository.findAllById(ids);
        //products.forEach(product -> product..setPrice(price));
        repository.saveAll(products);
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

                // Validación de formato: 4 dígitos numéricos
                if (!trimmed.matches("\\d{4}")) {
                    throw new SaveRecordException("El código de barras debe ser numérico de 4 dígitos.");
                }

                // Validación: debe iniciar con el dígito configurado
                if (!trimmed.startsWith(barcodeStartDigit)) {
                    throw new SaveRecordException(format("El código de barras debe iniciar con '%s'.", barcodeStartDigit));
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

            // Si no se envía barcode, generar el siguiente automáticamente
            String lastBarcode = findLastBarcodeStartingWithConfiguredDigit();
            int baseNumber;
            try {
                baseNumber = Integer.parseInt(barcodeStartDigit + "00");
            } catch (NumberFormatException e) {
                logger.warn("Configuración 'barcodeStartDigit' no numérica: {}", barcodeStartDigit);
                baseNumber = 2800; // Fallback seguro
            }

            int nextNumber = baseNumber;
            if (lastBarcode != null && !lastBarcode.isEmpty() && lastBarcode.matches("\\d{4}")) {
                try {
                    int currentNumber = Integer.parseInt(lastBarcode);
                    nextNumber = currentNumber + 1;
                } catch (NumberFormatException e) {
                    logger.warn("El último barcode no es numérico válido: {}", lastBarcode);
                    nextNumber = baseNumber;
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

    private String findLastBarcodeStartingWithConfiguredDigit() {
        String barcode = repository.findHighestBarcodeAsString();
        if (barcode != null && barcode.startsWith(barcodeStartDigit)) {
            return barcode;
        }
        return null;
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
            //existBarcode(dto.getBarcode());
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
        Product product = mapper.mapToEntity(dto);
        product.setId(id);
        product = repository.save(product);
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
