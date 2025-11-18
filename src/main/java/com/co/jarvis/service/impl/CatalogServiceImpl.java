package com.co.jarvis.service.impl;

import com.co.jarvis.dto.CatalogDto;
import com.co.jarvis.entity.Catalog;
import com.co.jarvis.repository.CatalogRepository;
import com.co.jarvis.service.CatalogService;
import com.co.jarvis.util.exception.ResourceNotFoundException;
import com.co.jarvis.util.mappers.GenericMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.co.jarvis.enums.CatalogType.BRAND;
import static com.co.jarvis.enums.CatalogType.CATEGORY;

@Service
public class CatalogServiceImpl implements CatalogService {


    @Autowired
    private CatalogRepository catalogRepository;

    GenericMapper<Catalog, CatalogDto> mapper = new GenericMapper<>(Catalog.class, CatalogDto.class);

    public List<String> getAllBrands() {
        return catalogRepository.findByType(BRAND)
                .stream()
                .map(Catalog::getValue)
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getAllCategories() {
        return catalogRepository.findByType(CATEGORY)
                .stream()
                .map(Catalog::getValue)
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogDto> findAll() {
        return mapper.mapToDtoList(catalogRepository.findAll());
    }

    @Override
    public CatalogDto findById(String id) {
        return mapper.mapToDto(catalogRepository.findById(id).orElse(null));
    }

    @Override
    public CatalogDto save(CatalogDto dto) {
        return mapper.mapToDto(catalogRepository.save(mapper.mapToEntity(dto)));
    }

    @Override
    public void deleteById(String id) {
        catalogRepository.deleteById(id);
    }

    @Override
    public CatalogDto update(CatalogDto dto, String id) {

        if(!catalogRepository.existsById(id)) {
            throw new ResourceNotFoundException("El catálogo " + dto.getValue() + " no existe.");
        }

        dto.setId(id);
        return mapper.mapToDto(catalogRepository.save(mapper.mapToEntity(dto)));
    }
}
