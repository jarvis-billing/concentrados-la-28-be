package com.co.jarvis.repository;

import com.co.jarvis.entity.Person;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PersonRepository extends MongoRepository<Person, String> {

    List<Person> findByDocumentNumber(String documentNumber);
}