package com.ordermanagement.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A new order, as accepted by this service. The order id is generated here so the
 * client gets it back immediately, before any consumer has processed the order.
 */
public record Order(String id, Customer customer, List<OrderItem> items, BigDecimal totalAmount, Instant createdAt) {

    public Order {
        items = List.copyOf(items);
    }

    public static Order create(Customer customer, List<OrderItem> items, Instant createdAt) {
        BigDecimal totalAmount = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(UUID.randomUUID().toString(), customer, items, totalAmount, createdAt);
    }
}
