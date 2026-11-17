package com.ordermanagement.order.exception;

import java.time.Duration;

/**
 * Thrown without calling Kafka while the circuit breaker is open, because recent publishes kept failing.
 * The client is told when it is worth trying again.
 */
public class PublishingSuspendedException extends OrderPublishingException {

    private final Duration retryAfter;

    public PublishingSuspendedException(String orderId, Duration retryAfter, Throwable cause) {
        super(orderId, cause);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
