package com.co.jarvis.service;

import com.co.jarvis.dto.ProductVatTypeDto;
import com.co.jarvis.enums.EVat;

public interface ProductVatTypeService extends BaseService<ProductVatTypeDto> {

    ProductVatTypeDto findByTipoIva(EVat eVat);
}
