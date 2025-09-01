package com.co.jarvis.controller;

import com.co.jarvis.dto.PersonDto;
import com.co.jarvis.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/person", produces = MediaType.APPLICATION_JSON_VALUE)
public class PersonController extends GenericController<PersonDto, PersonService> {

    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);

    @Autowired
    private PersonService service;

    @Override
    protected PersonService getService() {
        return service;
    }

    @GetMapping("/findByDocumentNumber/{documentNumber}")
    public ResponseEntity<List<PersonDto>> findByDocumentNumber(@PathVariable String documentNumber) {
        logger.info("PersonController -> findByDocumentNumber");
        List<PersonDto> response = service.findByDocumentNumber(documentNumber);
        return ResponseEntity.ok(response);
    }

}
