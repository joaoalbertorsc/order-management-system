package com.ordermanagement.processing.exception;

import com.ordermanagement.processing.domain.OrderStatus;

/**
 * Thrown when a status change is not allowed by the order lifecycle (e.g. DELIVERED back to CREATED).
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final OrderStatus currentStatus;
    private final OrderStatus requestedStatus;

    public InvalidStatusTransitionException(String orderId, OrderStatus currentStatus, OrderStatus requestedStatus) {
        super("Order %s cannot change from %s to %s".formatted(orderId, currentStatus, requestedStatus));
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getRequestedStatus() {
        return requestedStatus;
    }
}
