package com.co.jarvis.service.impl;

import com.co.jarvis.dto.CompanyDto;
import com.co.jarvis.entity.Company;
import com.co.jarvis.enums.EStatus;
import com.co.jarvis.repository.CompanyRepository;
import com.co.jarvis.service.CompanyService;
import com.co.jarvis.util.exception.*;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyServiceImpl implements CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private CompanyRepository repository;

    GenericMapper<Company, CompanyDto> mapper = new GenericMapper<>(Company.class, CompanyDto.class);

    @Override
    public List<CompanyDto> findAll() {
        return mapper.mapToDtoList(repository.findAll());
    }

    @Override
    public CompanyDto findById(String id) {
        return mapper.mapToDto(repository.findById(id).get());
    }

    @Override
    public CompanyDto save(CompanyDto dto) {
        try {
            companyExist(dto);
            return saveCompany(dto);
        } catch (DuplicateRecordException e) {
            logger.error("CompanyServiceImpl -> save -> ERROR: La empresa ya se encuentra creada");
            throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR);
        } catch (Exception e) {
            logger.error("CompanyServiceImpl -> save -> ERROR: Inesperado");
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR);
        }
    }

    private CompanyDto saveCompany(CompanyDto dto) {
        Company entityBd = mapper.mapToEntity(dto);
        entityBd = repository.save(entityBd);
        dto = mapper.mapToDto(entityBd);
        return dto;
    }

    private void companyExist(CompanyDto dto) {
        repository.findAll().forEach(company -> {
            if (company.getNit().equalsIgnoreCase(dto.getNit())) {
                throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR);
            }
        });
    }

    @Override
    public void deleteById(String id) {
        try {
            repository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            logger.error("CompanyServiceImpl -> deleteById -> ERROR: La empresa no se encuentra");
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        } catch (DeleteRecordException e) {
            logger.error("CompanyServiceImpl -> deleteById -> ERROR: Eliminando la empresa");
            throw new DeleteRecordException(MessageConstants.DELETE_RECORD_ERROR, e.getCause());
        }
    }

    @Override
    public CompanyDto update(CompanyDto dto, String id) {
        try {
            Optional<Company> opEntity = repository.findById(id);
            Company entity = opEntity.orElseThrow(()
                    -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));

            if (!entity.getNit().equalsIgnoreCase(dto.getNit())) {
                companyExist(dto);
            }
            dto.setId(id);
            return saveCompany(dto);
        } catch (ResourceNotFoundException e) {
            logger.error("CompanyServiceImpl -> update -> ERROR: No se encuentra la empresa");
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND, e);
        } catch (DuplicateRecordException e) {
            logger.error("CompanyServiceImpl -> update -> ERROR: La empresa ya se encuentra creada");
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            logger.error("CompanyServiceImpl -> update -> ERROR: Inesperado");
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e);
        }
    }

    @Override
    public CompanyDto findByStatus(EStatus status) {
        try {
            Company company = repository.findByStatus(status);
            if (company == null && status.equals(EStatus.ACTIVO)) {
                throw new ResourceInactiveException(MessageConstants.RESOURCE_INACTIVE);
            }
            return mapper.mapToDto(company);
        } catch (ResourceNotFoundException e) {
            logger.error("CompanyServiceImpl -> findByEstado -> ERROR: La empresa no se encuentra activa");
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            logger.error("CompanyServiceImpl -> findByEstado -> Erro Inesperado {}", e.getMessage());
            throw new GenericInternalException(MessageConstants.GENERIC_ERROR);
        }
    }
}
