package com.co.jarvis.service.impl;

import com.co.jarvis.dto.PersonDto;
import com.co.jarvis.entity.Person;
import com.co.jarvis.repository.PersonRepository;
import com.co.jarvis.service.PersonService;
import com.co.jarvis.util.exception.DeleteRecordException;
import com.co.jarvis.util.exception.DuplicateRecordException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
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
public class PersonServiceImpl implements PersonService {

    private static final Logger logger = LoggerFactory.getLogger(PersonServiceImpl.class);
    public static final String ND = ": El número de documento ya existe";

    GenericMapper<Person, PersonDto> mapper
            = new GenericMapper<>(Person.class, PersonDto.class);

    @Autowired
    private PersonRepository repository;

    @Override
    public List<PersonDto> findAll() {
        logger.info("PersonServiceImpl -> findAll");
        List<Person> lstBd = repository.findAll();
        return mapper.mapToDtoList(lstBd);
    }

    @Override
    public PersonDto save(PersonDto dto) {
        logger.info("PersonServiceImpl -> save");
        try {
            validateDocumentNumber(dto.getDocumentNumber());

            Person entityBd = mapper.mapToEntity(dto);
            entityBd = repository.save(entityBd);
            dto = mapper.mapToDto(entityBd);
            return dto;
        } catch (DuplicateRecordException e) {
            logger.error("PersonServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            logger.error("PersonServiceImpl -> save -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR);
        }
    }

    @Override
    public PersonDto findById(String id) {
        logger.info("PersonServiceImpl -> findById");
        Optional<Person> opEntity = repository.findById(id);
        return opEntity.map(person -> mapper.mapToDto(person)).orElse(null);
    }

    @Override
    public void deleteById(String id) {
        logger.info("PersonServiceImpl -> deleteById");
        try {
            repository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            logger.error("PersonServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            logger.error("PersonServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new DeleteRecordException(MessageConstants.DELETE_RECORD_ERROR);
        }
    }

    @Override
    public PersonDto update(PersonDto dto, String id) {
        logger.info("PersonServiceImpl -> update");
        try{
            Optional<Person> opEntity = repository.findById(id);
            Person entity = opEntity.orElseThrow(()
                    -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));
            if (!entity.getDocumentNumber().equalsIgnoreCase(dto.getDocumentNumber())) {
                validateDocumentNumber(entity.getDocumentNumber());
            }
            return updatePresent(dto, entity.getId());
        } catch (ResourceNotFoundException e) {
            logger.error("PersonServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND, e);
        } catch (DuplicateRecordException e) {
            logger.error("PersonServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e){
            logger.error("PersonServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e);
        }
    }

    @Override
    public List<PersonDto> findByDocumentNumber(String documentNumber) {
        logger.info("PersonServiceImpl -> findByDocumentNumber");
        List<Person> lstBd = repository.findByDocumentNumber(documentNumber);
        return mapper.mapToDtoList(lstBd);
    }

    private PersonDto updatePresent(PersonDto dto, String id) {
        logger.info("PersonServiceImpl -> updatePresent");
        Person entity = mapper.mapToEntity(dto);
        entity.setId(id);
        entity = repository.save(entity);
        return mapper.mapToDto(entity);
    }

    private void validateDocumentNumber(String documentNumber) {
        List<Person> list = repository.findByDocumentNumber(documentNumber);
        if (list != null && !list.isEmpty()){
            throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR + ND);
        }
    }
}