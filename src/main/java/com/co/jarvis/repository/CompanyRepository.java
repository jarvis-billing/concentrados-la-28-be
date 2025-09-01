package com.co.jarvis.repository;

import com.co.jarvis.entity.Company;
import com.co.jarvis.enums.EStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyRepository extends MongoRepository<Company, String> {

    Company findByStatus(EStatus status);
}
