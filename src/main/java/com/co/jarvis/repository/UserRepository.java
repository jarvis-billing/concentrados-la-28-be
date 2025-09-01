package com.co.jarvis.repository;

import com.co.jarvis.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByNumberIdentity(String numberIdentity);
}
