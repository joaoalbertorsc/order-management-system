package com.ordermanagement.processing.api.dto;

import com.ordermanagement.processing.domain.Order;
import com.ordermanagement.processing.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String customerName,
        String customerEmail,
        List<Item> items,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public record Item(String productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(item -> new Item(
                        item.productId(), item.productName(), item.quantity(), item.unitPrice(), item.subtotal()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                items,
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
