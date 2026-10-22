package com.ordermanagement.processing.exception;

/**
 * Thrown when a message read from Kafka does not carry a usable order.
 * Retrying cannot fix it, so the consumer skips the record instead.
 */
public class InvalidOrderEventException extends RuntimeException {

    public InvalidOrderEventException(String orderId, String details) {
        super("Invalid order event %s: %s".formatted(orderId, details));
    }
}
