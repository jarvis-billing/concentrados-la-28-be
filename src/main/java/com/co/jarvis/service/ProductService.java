package com.co.jarvis.service;

import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.ProductDto;
import com.co.jarvis.entity.Product;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService extends BaseService<ProductDto> {

    ProductDto findByBarcode(String barcode);

    PaginationDto<ProductDto> findAllPage(int pageNumber, int pageSize);

    PaginationDto<ProductDto> findAllPageSearch(int pageNumber, int pageSize, String search);

    void updatePriceByIds(BigDecimal price, List<String> ids);
}
