package com.co.jarvis.service.impl;

import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.ProductDto;
import com.co.jarvis.entity.Product;
import com.co.jarvis.repository.ProductRepository;
import com.co.jarvis.service.ProductService;
import com.co.jarvis.util.exception.*;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository repository;

    GenericMapper<Product, ProductDto> mapper = new GenericMapper<>(Product.class, ProductDto.class);
    PaginationMapper<Product, ProductDto> paginationMapper = new PaginationMapper<>(Product.class, ProductDto.class);

    @Override
    public ProductDto findByBarcode(String barcode) {
        log.info("ProductServiceImpl -> findByBarcode");
        try {
            validateBarcode(barcode);
            Product entity = repository.findByBarcode(barcode);
            if (entity == null) {
                throw new ResourceNotFoundException(MessageConstants.PRODUCT_NOT_FOUND + barcode);
            }
            return mapper.mapToDto(entity);
        } catch (NotBarcodeException e) {
            logger.error("ProductServiceImpl -> findByBarcode -> Error: Barcode empty");
            throw new NotBarcodeException(e.getMessage());
        }  catch (ResourceNotFoundException e) {
            logger.error("ProductServiceImpl -> findByBarcode -> Error: Product not Found");
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            logger.error("ProductServiceImpl -> findByBarcode -> Ocurrio un error en la busqueda del producto");
            throw new GenericInternalException(MessageConstants.GENERIC_ERROR);
        }
    }

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
        Page<Product> pageProduct = repository.findByBarcodeOrDescriptionContainingIgnoreCase(search, search, pageable);
        return paginationMapper.pageToPagination(pageProduct);
    }

    @Override
    public void updatePriceByIds(BigDecimal price, List<String> ids) {
        log.info("ProductServiceImpl -> updatePriceByIds");
        List<Product> products = repository.findAllById(ids);
        products.forEach(x -> x.setPrice(price));
        repository.saveAll(products);
    }

    private static void validateBarcode(String barcode) {
        if (barcode == null || barcode.trim().equalsIgnoreCase("")){
            throw new NotBarcodeException(MessageConstants.NOT_BARCODE);
        }
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
            existBarcode(dto.getBarcode());
            Product entityBd = mapper.mapToEntity(dto);
            entityBd = repository.save(entityBd);
            return mapper.mapToDto(entityBd);
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
    public ProductDto update(ProductDto dto, String id) {
        log.info("ProductServiceImpl -> update");
        try {
            Optional<Product> opEntity = repository.findById(id);
            Product entity = opEntity.orElseThrow(()
                    -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));
            if (!entity.getId().equalsIgnoreCase(dto.getId())) {
                validateProductExist(entity.getBarcode());
            }
            return updatePresent(dto, entity.getId());
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

    private void validateProductExist(String id) {
        Optional<Product> product = repository.findById(id);
        if (product.isPresent()) {
            throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR);
        }
    }

    private void existBarcode(String barcode) {
        Product product = repository.findByBarcode(barcode);
        if (product != null) {
            update(mapper.mapToDto(product), product.getId());
        }
    }

    private ProductDto updatePresent(ProductDto dto, String id) {
        log.info("ProductServiceImpl -> updatePresent");
        Product entity = mapper.mapToEntity(dto);
        entity.setId(id);
        entity = repository.save(entity);
        return mapper.mapToDto(entity);
    }
}
