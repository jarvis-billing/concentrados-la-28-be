package com.co.jarvis.controller;

import com.co.jarvis.dto.CompanyDto;
import com.co.jarvis.enums.EStatus;
import com.co.jarvis.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/company", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompanyController extends GenericController<CompanyDto, CompanyService> {

    private static final Logger logger = LoggerFactory.getLogger(CompanyController.class);

    @Autowired
    private CompanyService service;

    @Override
    protected CompanyService getService() {
        return service;
    }

    @GetMapping("/findByStatus")
    public CompanyDto findByStatus() {
        logger.info("CompanyController -> findByStatus");
        return service.findByStatus(EStatus.ACTIVO);
    }
}
