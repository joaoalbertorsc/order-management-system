package com.ordermanagement.order.exception;

/**
 * Thrown when an order cannot be delivered to Kafka (broker down, timeout, serialization error...).
 */
public class OrderPublishingException extends RuntimeException {

    private final String orderId;

    public OrderPublishingException(String orderId, Throwable cause) {
        super("Could not publish order " + orderId + " to Kafka", cause);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
