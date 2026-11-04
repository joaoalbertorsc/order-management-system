package com.ordermanagement.processing.messaging;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * This service's view of the message published by the order-service on the orders topic.
 * Unknown JSON fields are ignored, so the producer can add fields without breaking this consumer.
 */
public record OrderCreatedEvent(
        @NotBlank String orderId,
        @NotBlank String customerName,
        @NotBlank String customerEmail,
        @NotEmpty List<@Valid @NotNull Item> items,
        @NotNull @PositiveOrZero BigDecimal totalAmount,
        @NotNull Instant createdAt) {

    public record Item(
            @NotBlank String productId,
            @NotBlank String productName,
            @Positive int quantity,
            @NotNull @PositiveOrZero BigDecimal unitPrice) {
    }
}
