package com.ordermanagement.order.service;

import com.ordermanagement.order.domain.Customer;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.domain.OrderItem;
import com.ordermanagement.order.messaging.OrderEventPublisher;
import com.ordermanagement.order.validation.OrderValidationRule;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

    private final List<OrderValidationRule> validationRules;
    private final OrderEventPublisher eventPublisher;
    private final Clock clock;

    public OrderService(List<OrderValidationRule> validationRules, OrderEventPublisher eventPublisher, Clock clock) {
        this.validationRules = validationRules;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Builds a new order, checks it against every business rule and publishes it for the downstream services.
     */
    public Order createOrder(Customer customer, List<OrderItem> items) {
        Order order = Order.create(customer, items, Instant.now(clock));
        validationRules.forEach(rule -> rule.validate(order));
        eventPublisher.publishOrderCreated(order);
        return order;
    }
}
