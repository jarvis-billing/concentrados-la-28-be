package com.co.jarvis.service.impl;

import com.co.jarvis.dto.ClientDto;
import com.co.jarvis.entity.Client;
import com.co.jarvis.enums.EDocument;
import com.co.jarvis.repository.ClientRepository;
import com.co.jarvis.service.ClientService;
import com.co.jarvis.util.SearchCriteriaClient;
import com.co.jarvis.util.exception.DeleteRecordException;
import com.co.jarvis.util.exception.DuplicateRecordException;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.exception.SaveRecordException;
import com.co.jarvis.util.mappers.GenericMapper;
import com.co.jarvis.util.mensajes.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ClientServiceImpl implements ClientService {
    private static final Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);
    GenericMapper<Client, ClientDto> mapper = new GenericMapper<>(Client.class, ClientDto.class);

    private ClientRepository repository;

    private ExecutorService executorService;

    @Value("${default-client.identity.number}")
    private String numberIdentityClientDefault;

    @Autowired
    public ClientServiceImpl(ClientRepository clientRepository) {
        this.repository = clientRepository;
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public CompletableFuture<ClientDto> findByCriteria(
            String idNumber, String documentType) {
        return CompletableFuture.supplyAsync(() -> {
            String eDocument = EDocument.getByValue(documentType).name();
            Client clients = repository.findByIdNumberAndDocumentType(idNumber, eDocument);
            if (clients == null) {
                throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
            }
            return mapper.mapToDto(clients);
        }, executorService);
    }

    @Override
    public ClientDto findByDocument(SearchCriteriaClient searchCriteriaClient) {
        logger.info("ClientServiceImpl -> findByDocument");
        Client client = repository.findByIdNumberAndDocumentType(
                searchCriteriaClient.getIdNumber(), searchCriteriaClient.getDocumentType());
        if (client == null || client.getId() == null) {
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        }
        return mapper.mapToDto(client);
    }

    @Override
    public ClientDto getClientDefault() {
        Client clientDefault = repository.findByIdNumberAndDocumentType(numberIdentityClientDefault, EDocument.CEDULA_CIUDADANIA.name());
        return mapper.mapToDto(clientDefault);
    }

    @Override
    public List<ClientDto> findAll() {
        return mapper.mapToDtoList(repository.findAll());
    }

    @Override
    public ClientDto findById(String id) {
        return mapper.mapToDto(repository.findById(id).get());
    }

    @Override
    public ClientDto save(ClientDto dto) {
        log.info("ClientServiceImpl -> save");
        try {
            existClient(dto.getIdNumber());
            return saveOrUpdate(dto, null);
        } catch (Exception error) {
            log.error("No fue posible registrar el cliente. verificar los log: {}", error.getMessage());
            throw new SaveRecordException(MessageConstants.SAVE_RECORD_ERROR);
        }
    }

    @Override
    public void deleteById(String id) {
        log.info("ClientServiceImpl -> deleteById");
        try {
            repository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            log.error("ClientServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("ClientServiceImpl -> deleteById -> ERROR: {}", e.getMessage());
            throw new DeleteRecordException(MessageConstants.DELETE_RECORD_ERROR);
        }
    }

    @Override
    public ClientDto update(ClientDto dto, String id) {
        log.info("ClientServiceImpl -> update");
        try {
            Optional<Client> opEntity = repository.findById(id);
            Client entity = opEntity.orElseThrow(()
                    -> new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND));
            if (!entity.getId().equalsIgnoreCase(dto.getId())) {
                existClient(entity.getIdNumber());
            }
            return saveOrUpdate(dto, entity.getId());
        } catch (ResourceNotFoundException e) {
            log.error("ClientServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new ResourceNotFoundException(MessageConstants.RESOURCE_NOT_FOUND, e);
        } catch (DuplicateRecordException e) {
            log.error("ClientServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new DuplicateRecordException(e.getMessage());
        } catch (Exception e) {
            log.error("ClientServiceImpl -> update -> ERROR: {}", e.getMessage());
            throw new SaveRecordException(MessageConstants.UPDATE_RECORD_ERROR, e);
        }
    }

    private void existClient(String idNumber) {
        boolean existClient = repository.existsByIdNumber(idNumber);
        if (existClient) {
            throw new DuplicateRecordException(MessageConstants.DUPLICATE_RECORD_ERROR);
        }
    }

    private ClientDto saveOrUpdate(ClientDto dto, String id) {
        Client client = mapper.mapToEntity(dto);
        client.setId(id);
        return mapper.mapToDto(repository.save(client));
    }
}
