package com.co.jarvis.controller;

import com.co.jarvis.service.BaseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;

public abstract class GenericController<D extends Serializable, S extends BaseService<D>> {

    private static final Logger logger = LoggerFactory.getLogger(GenericController.class);

    protected abstract S getService();

    @GetMapping
    public ResponseEntity<List<D>> findAll() {
        logger.info("BaseController -> findAll");
        List<D> dataList = getService().findAll();
        return ResponseEntity.ok(dataList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<D> findById(@PathVariable String id) {
        logger.info("BaseController -> findById");
        D result = getService().findById(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<D> save(@Valid @RequestBody D dto) {
        logger.info("BaseController -> save");
        dto = getService().save(dto);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<D> update(@Validated @RequestBody D dto, @PathVariable String id) {
        logger.info("BaseController -> update");
        if (id != null) {
            dto = getService().update(dto, id);
        }
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        logger.info("BaseController -> delete");
        getService().deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
