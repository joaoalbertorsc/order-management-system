package com.ordermanagement.order.messaging;

import com.ordermanagement.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Message published to the orders topic. This is the public contract between services:
 * consumers keep their own copy of it, so changes here must stay backward compatible.
 */
public record OrderCreatedEvent(
        String orderId,
        String customerName,
        String customerEmail,
        List<Item> items,
        BigDecimal totalAmount,
        Instant createdAt) {

    public record Item(String productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    public static OrderCreatedEvent from(Order order) {
        List<Item> items = order.items().stream()
                .map(item -> new Item(item.productId(), item.productName(), item.quantity(), item.unitPrice()))
                .toList();
        return new OrderCreatedEvent(
                order.id(),
                order.customer().name(),
                order.customer().email(),
                items,
                order.totalAmount(),
                order.createdAt());
    }
}
