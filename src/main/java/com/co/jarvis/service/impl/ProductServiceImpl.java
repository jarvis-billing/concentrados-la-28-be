package com.co.jarvis.service.impl;

import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.ProductDto;
import com.co.jarvis.entity.Presentation;
import com.co.jarvis.entity.Product;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.service.ProductService;
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
import java.util.List;
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
        return paginationMapper.pageToPagination(pageProduct);
    }

    @Override
    public PaginationDto<ProductDto> findAllPageSearch(int pageNumber, int pageSize, String search) {
        log.info("ProductServiceImpl -> findAllPageSearch");
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Product> pageProduct = repository.findByPresentationsBarcodeOrDescriptionContainingIgnoreCase(search, search, pageable);
        return paginationMapper.pageToPagination(pageProduct);
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
        return product != null ? mapper.mapToDto(product) : null;
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
        // Verifica si el barcode existe
        ProductDto product = null;
        if (barcode != null && !barcode.isEmpty()) {
            product = findByPresentationsBarcode(barcode);
        }

        if (product != null) {
            Presentation presentation = product.getPresentations().stream()
                    .filter(p -> p.getBarcode().equals(barcode)).findFirst()
                    .orElse(null);
            assert presentation != null;
            throw new DuplicateRecordException(format(
                    "El código de barras %s ya existe. Está assignation al producto: %s en la presentacion: %s", barcode,
                    product.getDescription(), presentation.getLabel()));
        }

        String lastBarcode = findLastBarcodeStartingWithConfiguredDigit();
        int nextNumber = 2800; // Valor inicial si no existe ninguno
        if (lastBarcode != null && lastBarcode.startsWith(barcodeStartDigit) && lastBarcode.length() == 4) {
            nextNumber = Integer.parseInt(lastBarcode) + 1;
        }

        return format("%04d", nextNumber);
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
        Product product = repository.findTopByOrderByPresentationsBarcodeDesc();
        return product != null ? product.getPresentations().get(0).getBarcode() : null;
    }

    @Override
    public List<ProductDto> findAll() {
        log.info("ProductServiceImpl -> findAll");
        return mapper.mapToDtoList(repository.findAll());
    }

    @Override
    public ProductDto findById(String id) {
        Optional<Product> producto = repository.findById(id);
        return producto.map(product -> mapper.mapToDto(product)).orElse(null);
    }

    @Override
    public ProductDto save(ProductDto dto) {
        log.info("ProductServiceImpl -> save");
        try {
            //existBarcode(dto.getBarcode());
            Product product = mapper.mapToEntity(dto);
            product = repository.save(product);
            return mapper.mapToDto(product);
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
        return mapper.mapToDto(product);
    }
}
