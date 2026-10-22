package com.ordermanagement.processing.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Order " + orderId + " was not found");
    }
}
