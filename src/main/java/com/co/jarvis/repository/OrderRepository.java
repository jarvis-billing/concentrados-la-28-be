package com.co.jarvis.repository;

import com.co.jarvis.entity.Order;
import com.co.jarvis.entity.User;
import com.co.jarvis.enums.EStatusOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    Order findFirstByOrderNumber(Long orderNumber);

    Order findFirstByOrderByCreationDateDesc();

    List<Order> findByStatusAndCreationUser(EStatusOrder eStatusOrder, User creationUser);

    Page<Order> findByStatus(EStatusOrder eStatusOrder, Pageable pageable);
}