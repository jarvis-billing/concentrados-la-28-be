package com.co.jarvis.controller;

import com.co.jarvis.dto.CatalogDto;
import com.co.jarvis.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogController extends GenericController<CatalogDto, CatalogService> {

    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);

    @Autowired
    private CatalogService service;

    @Override
    protected CatalogService getService() {
        return service;
    }

    @PostMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCategoryCatalog(@RequestBody CatalogDto dto) {
        service.save(dto);
        return ResponseEntity.ok(service.getAllCategories());
    }

    @PostMapping(value = "/brands", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createBrandCatalog(@RequestBody CatalogDto dto) {
        service.save(dto);
        return ResponseEntity.ok(service.getAllBrands());
    }

    @PutMapping(value = "/brands/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateBrandCatalog(@RequestBody CatalogDto dto, @PathVariable String id) {
        return ResponseEntity.ok(service.update(dto, id));
    }

    @DeleteMapping(value = "/brands/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteBrandCatalog(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        logger.info("ProductController -> getAllCategories");
        try {
            return ResponseEntity.ok(service.getAllCategories());
        } catch (Exception ex) {
            logger.error("ProductController -> getAllCategories -> Error interno del servidor: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Ocurrió un error interno en el servidor."));
        }
    }

    @GetMapping("/brands")
    public ResponseEntity<?> getAllBrands() {
        logger.info("ProductController -> getAllBrands");
        try {
            return ResponseEntity.ok(service.getAllBrands());
        } catch (Exception ex) {
            logger.error("ProductController -> getAllBrands -> Error interno del servidor: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Ocurrió un error interno en el servidor."));
        }
    }
}
