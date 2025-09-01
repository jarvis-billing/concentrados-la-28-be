package com.co.jarvis.util.mappers;

import org.modelmapper.ModelMapper;
import java.util.List;
import java.util.stream.Collectors;

public class GenericMapper<Entity, Dto> {

    private static final ModelMapper modelMapper = new ModelMapper();
    private final Class<Entity> entityClass;
    private final Class<Dto> dtoClass;

    public GenericMapper(Class<Entity> entityClass, Class<Dto> dtoClass) {
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
    }

    public Dto mapToDto(Entity entity) {
        return modelMapper.map(entity, dtoClass);
    }

    public Entity mapToEntity(Dto dto) {
        return modelMapper.map(dto, entityClass);
    }

    public List<Dto> mapToDtoList(List<Entity> entityList) {
        return entityList.stream()
                .map(entity -> modelMapper.map(entity, dtoClass))
                .collect(Collectors.toList());
    }

    public List<Entity> mapToEntityList(List<Dto> dtoList) {
        return dtoList.stream()
                .map(dto -> modelMapper.map(dto, entityClass))
                .collect(Collectors.toList());
    }
}

