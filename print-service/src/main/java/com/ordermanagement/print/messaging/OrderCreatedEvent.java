package com.ordermanagement.print.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * This service's view of the message published by the order-service on the orders topic.
 * Unknown JSON fields are ignored, so the producer can add fields without breaking this consumer.
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
}
