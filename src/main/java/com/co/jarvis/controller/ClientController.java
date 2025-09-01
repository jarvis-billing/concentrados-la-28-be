package com.co.jarvis.controller;

import com.co.jarvis.dto.ClientDto;
import com.co.jarvis.util.SearchCriteriaClient;
import com.co.jarvis.service.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/client", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClientController extends GenericController<ClientDto, ClientService> {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    private ClientService service;

    @Override
    protected ClientService getService() {
        return service;
    }

    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientDto> searchClient(@RequestBody SearchCriteriaClient searchCriteria) {
        logger.info("ClientController -> searchClient");
        ClientDto clientDto = service.findByCriteria(
                searchCriteria.getIdNumber(),
                searchCriteria.getDocumentType().toUpperCase()).join();
        return ResponseEntity.ok(clientDto);
    }

    @PostMapping(value = "/findByDocument", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientDto> findByDocument(@RequestBody SearchCriteriaClient searchCriteria) {
        logger.info("ClientController -> findByDocument");
        ClientDto clientDto = service.findByDocument(searchCriteria);
        return ResponseEntity.ok(clientDto);
    }
}
