package com.co.jarvis.service;

import java.math.BigDecimal;

import com.co.jarvis.dto.BulkPresentationPriceUpdateRequest;
import com.co.jarvis.dto.BulkPresentationPriceUpdateResponse;
import com.co.jarvis.dto.DisplayStock;
import com.co.jarvis.dto.PaginationDto;
import com.co.jarvis.dto.ProductDto;
import com.co.jarvis.dto.UserDto;
import com.co.jarvis.entity.Product;

public interface ProductService extends BaseService<ProductDto> {

    PaginationDto<ProductDto> findAllPage(int pageNumber, int pageSize);

    PaginationDto<ProductDto> findAllPageSearch(int pageNumber, int pageSize, String search);

    /**
     * Actualiza en bloque los precios (venta y/o costo) de presentaciones específicas
     * identificadas por productId + barcode. Cada producto se procesa de forma atómica;
     * los errores son reportados parcialmente sin detener el resto del procesamiento.
     */
    BulkPresentationPriceUpdateResponse bulkUpdatePresentationPrices(
            BulkPresentationPriceUpdateRequest request, UserDto user);

    ProductDto findByPresentationsBarcode(String barcode);

    void decreaseStock(Product product, BigDecimal amount);

    void increaseStock(Product product, BigDecimal amount);

    String validateOrGenerateBarcode(String barcode);

    String generateNextProductCode();

    DisplayStock computeDisplayStock(Product product);

}
