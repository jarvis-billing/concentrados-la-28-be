package com.co.jarvis.service;

import java.math.BigDecimal;
import java.util.List;

import com.co.jarvis.dto.DisplayStock;
import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.ProductDto;
import com.co.jarvis.entity.Product;

public interface ProductService extends BaseService<ProductDto> {

    PaginationDto<ProductDto> findAllPage(int pageNumber, int pageSize);

    PaginationDto<ProductDto> findAllPageSearch(int pageNumber, int pageSize, String search);

    void updatePriceByIds(BigDecimal price, List<String> ids);

    ProductDto findByPresentationsBarcode(String barcode);

    void decreaseStock(Product product, BigDecimal amount);

    void increaseStock(Product product, BigDecimal amount);

    String validateOrGenerateBarcode(String barcode);

    String generateNextProductCode();

    DisplayStock computeDisplayStock(Product product);

}
