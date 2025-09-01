package com.co.jarvis.service.impl;

import com.co.jarvis.dto.ProductVatTypeDto;
import com.co.jarvis.enums.EVat;
import com.co.jarvis.repository.ProductVatTypeRepository;
import com.co.jarvis.service.ProductVatTypeService;
import com.co.jarvis.util.exception.DuplicateRecordException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProductVatTypeServiceImpl implements ProductVatTypeService {

    @Autowired
    private ProductVatTypeRepository repository;

    GenericMapper<com.co.jarvis.entity.ProductVatType, ProductVatTypeDto> mapper = new GenericMapper<>(com.co.jarvis.entity.ProductVatType.class, ProductVatTypeDto.class);

    @Override
    public ProductVatTypeDto findByTipoIva(EVat eVat) {
        return mapper.mapToDto(repository.findByVatType(eVat));
    }

    @Override
    public List<ProductVatTypeDto> findAll() {
        return mapper.mapToDtoList(repository.findAll());
    }

    @Override
    public ProductVatTypeDto findById(String id) {
        return mapper.mapToDto(repository.findById(id).get());
    }

    @Override
    public ProductVatTypeDto save(ProductVatTypeDto dto) {
        log.info("TipoIvaServiceImpl -> Create");
        try {
            ivaTypeExist(dto);
            return saveIvaType(dto);
        } catch (Exception error) {
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR, error);
        }
    }

    private static void ivaTypeExist(ProductVatTypeDto dto) {
        for (EVat eVat : EVat.values()) {
            if (eVat.name().equalsIgnoreCase(dto.getVatType().name())) {
                throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR);
            }
        }
    }

    private ProductVatTypeDto saveIvaType(ProductVatTypeDto dto) {
        com.co.jarvis.entity.ProductVatType productVatType = mapper.mapToEntity(dto);
        productVatType = repository.save(productVatType);
        return mapper.mapToDto(productVatType);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public ProductVatTypeDto update(ProductVatTypeDto dto, String id) {
        try {
            ProductVatTypeDto ivaTypeDto = findById(id);
            if (ivaTypeDto == null) {
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            dto.setId(ivaTypeDto.getId());
            return saveIvaType(dto);
        } catch (ResourceNotFoundException e) {
            log.error("-> update -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(e.getMessage());
        } catch (DuplicateRecordException e) {
            log.error("-> update -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            log.error("-> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e);
        }
    }
}
