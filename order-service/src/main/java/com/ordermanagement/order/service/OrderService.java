package com.ordermanagement.order.service;

import com.ordermanagement.order.domain.Customer;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.domain.OrderItem;
import com.ordermanagement.order.messaging.OrderEventPublisher;
import com.ordermanagement.order.validation.OrderValidationRule;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        // MongoDB keeps milliseconds only; truncating here makes every service show the same timestamp
        Instant createdAt = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
        Order order = Order.create(customer, items, createdAt);
        validationRules.forEach(rule -> rule.validate(order));
        eventPublisher.publishOrderCreated(order);
        return order;
    }
}
