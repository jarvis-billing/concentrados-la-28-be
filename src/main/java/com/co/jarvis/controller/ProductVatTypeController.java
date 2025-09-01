package com.co.jarvis.controller;

import com.co.jarvis.dto.ProductVatTypeDto;
import com.co.jarvis.service.ProductVatTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/product_vat_type", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductVatTypeController extends GenericController<ProductVatTypeDto, ProductVatTypeService> {

    private static final Logger logger = LoggerFactory.getLogger(ProductVatTypeController.class);

    @Autowired
    private ProductVatTypeService service;

    @Override
    protected ProductVatTypeService getService() {
        return service;
    }
}
