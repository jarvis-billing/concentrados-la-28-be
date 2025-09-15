package com.co.jarvis.service;

import com.co.jarvis.dto.CatalogDto;

import java.util.List;

public interface CatalogService extends BaseService<CatalogDto> {

    List<String> getAllBrands();

    List<String> getAllCategories();
}
