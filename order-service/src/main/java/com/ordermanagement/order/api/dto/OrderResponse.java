package com.ordermanagement.order.api.dto;

import com.ordermanagement.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String customerName,
        String customerEmail,
        List<Item> items,
        BigDecimal totalAmount,
        Instant createdAt) {

    public record Item(String productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.items().stream()
                .map(item -> new Item(
                        item.productId(), item.productName(), item.quantity(), item.unitPrice(), item.subtotal()))
                .toList();
        return new OrderResponse(
                order.id(),
                order.customer().name(),
                order.customer().email(),
                items,
                order.totalAmount(),
                order.createdAt());
    }
}
