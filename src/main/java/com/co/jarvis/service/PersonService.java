package com.co.jarvis.service;

import com.co.jarvis.dto.PersonDto;

import java.util.List;

public interface PersonService extends BaseService<PersonDto> {

    List<PersonDto> findByDocumentNumber(String cedula);
}
