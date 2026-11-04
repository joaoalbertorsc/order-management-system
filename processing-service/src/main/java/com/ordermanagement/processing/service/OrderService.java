package com.ordermanagement.processing.service;

import com.ordermanagement.processing.domain.Order;
import com.ordermanagement.processing.domain.OrderStatus;
import com.ordermanagement.processing.exception.OrderNotFoundException;
import com.ordermanagement.processing.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Reads stored orders and changes their status on behalf of the REST API.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    /**
     * @param status optional filter; {@code null} returns orders in every status
     */
    public Page<Order> findOrders(@Nullable OrderStatus status, Pageable pageable) {
        return status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
    }

    public Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public Order updateStatus(String orderId, OrderStatus newStatus) {
        Order order = findOrder(orderId);
        OrderStatus previousStatus = order.getStatus();
        order.changeStatus(newStatus, Instant.now(clock));
        Order saved = orderRepository.save(order);
        log.info("Order {} status changed from {} to {}", orderId, previousStatus, newStatus);
        return saved;
    }
}
