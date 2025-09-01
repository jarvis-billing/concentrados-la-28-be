package com.co.jarvis.controller;

import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.ProductDto;
import com.co.jarvis.dto.ProductPriceDto;
import com.co.jarvis.service.ProductService;
import com.co.jarvis.service.impl.LoginUserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/product", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController extends GenericController<ProductDto, ProductService> {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService service;

    @Autowired
    private LoginUserService loginUserService;

    @Override
    protected ProductService getService() {
        return service;
    }

    @GetMapping("/findByBarcode/{barcode}")
    public ResponseEntity<ProductDto> findByBarcode(@PathVariable String barcode) {
        logger.info("ProductController -> findByBarcode");
        logger.info("{}", loginUserService.getUserLoginContext());
        return ResponseEntity.ok(service.findByBarcode(barcode));
    }

    @GetMapping("/paginate")
    public ResponseEntity<PaginationDto<ProductDto>> findAllPage(
            @RequestParam(required = false, name = "page-number", defaultValue = "0") int pageNumber,
            @RequestParam(required = false, name = "page-size", defaultValue = "10") int pageSize
    ) {
        logger.info("ProductController -> findAllPage");
        return ResponseEntity.ok(service.findAllPage(pageNumber, pageSize));
    }

    @GetMapping("/paginateSearch/{search}")
    public ResponseEntity<PaginationDto<ProductDto>> findAllPageSearch(
            @RequestParam(required = false, name = "page-number", defaultValue = "0") int pageNumber,
            @RequestParam(required = false, name = "page-size", defaultValue = "10") int pageSize,
            @PathVariable String search
    ) {
        logger.info("ProductController -> findAllPageSearch");
        return ResponseEntity.ok(service.findAllPageSearch(pageNumber, pageSize,search));
    }

    @PostMapping("/updatePrice")
    public ResponseEntity<Void> updatePriceByIds(@Valid @RequestBody ProductPriceDto dto) {
        logger.info("BaseController -> save");
        getService().updatePriceByIds(dto.getPrice(), dto.getIds());
        return ResponseEntity.noContent().build();
    }
}
