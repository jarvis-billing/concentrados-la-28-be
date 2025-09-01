package com.co.jarvis.util.mappers;


import com.co.jarvis.dto.PaginationDto;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class PaginationMapper<Entity, Dto> {
    private static final ModelMapper modelMapper = new ModelMapper();
    private final Class<Dto> dtoClass;


    public PaginationMapper(Class<Entity> entityClass, Class<Dto> dtoClass) {
        this.dtoClass = dtoClass;
    }

    public PaginationDto<Dto> pageToPagination(Page<Entity> entity) {
        PaginationDto<Dto> paginationDto = new PaginationDto<>();
        paginationDto.setPage(entity.getPageable().getPageNumber());
        paginationDto.setPageSize(entity.getPageable().getPageSize());
        paginationDto.setQuantityPage(entity.getTotalPages());
        paginationDto.setTotalElements(entity.getTotalElements());
        paginationDto.setContent(mapToDtoList(entity.getContent()));
        return paginationDto;
    }

    public List<Dto> mapToDtoList(List<Entity> entityList) {
        return entityList.stream()
                .map(entity -> modelMapper.map(entity, dtoClass))
                .collect(Collectors.toList());
    }

}
