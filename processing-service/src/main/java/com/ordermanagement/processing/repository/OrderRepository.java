package com.ordermanagement.processing.repository;

import com.ordermanagement.processing.domain.Order;
import com.ordermanagement.processing.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, String> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
