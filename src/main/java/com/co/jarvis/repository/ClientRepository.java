package com.co.jarvis.repository;

import com.co.jarvis.entity.Client;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientRepository extends MongoRepository<Client, String> {

    Client findByIdNumberAndDocumentType(String idNumber, String documentType);

    boolean existsByIdNumber(String idNumber);
}
