package com.ordermanagement.order.exception;

/**
 * Thrown when an order is well-formed but breaks a business rule
 * (for example, a total above the allowed limit).
 */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
