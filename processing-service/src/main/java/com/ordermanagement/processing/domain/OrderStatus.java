package com.ordermanagement.processing.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Order lifecycle:
 * <pre>
 * CREATED -> CONFIRMED -> SHIPPED -> DELIVERED
 *    |          |
 *    +----------+-> CANCELLED
 * </pre>
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    public List<OrderStatus> allowedTransitions() {
        return Arrays.stream(values()).filter(this::canTransitionTo).toList();
    }
}
