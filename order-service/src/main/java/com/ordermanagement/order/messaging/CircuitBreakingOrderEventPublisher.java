package com.ordermanagement.order.messaging;

import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.exception.PublishingSuspendedException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Wraps the Kafka publisher in a circuit breaker. When most recent publishes failed, the circuit opens and
 * new orders are refused at once, instead of each request holding a thread for up to 10 seconds while the
 * producer waits for a broker that is down. After a pause, a few trial publishes decide whether to close it.
 */
@Primary
@Component
public class CircuitBreakingOrderEventPublisher implements OrderEventPublisher {

    static final String CIRCUIT_BREAKER_NAME = "kafka-publisher";

    private final OrderEventPublisher delegate;
    private final CircuitBreaker circuitBreaker;

    public CircuitBreakingOrderEventPublisher(KafkaOrderEventPublisher delegate, CircuitBreakerRegistry registry) {
        this.delegate = delegate;
        this.circuitBreaker = registry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }

    @Override
    public void publishOrderCreated(Order order) {
        try {
            circuitBreaker.executeRunnable(() -> delegate.publishOrderCreated(order));
        } catch (CallNotPermittedException e) {
            throw new PublishingSuspendedException(order.id(), waitInOpenState(), e);
        }
    }

    private Duration waitInOpenState() {
        return Duration.ofMillis(circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1));
    }
}
