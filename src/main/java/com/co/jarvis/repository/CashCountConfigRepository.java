package com.co.jarvis.repository;

import com.co.jarvis.entity.CashCountConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CashCountConfigRepository extends MongoRepository<CashCountConfig, String> {
}
