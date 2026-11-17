package com.ordermanagement.order.messaging;

import com.ordermanagement.order.domain.Customer;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.domain.OrderItem;
import com.ordermanagement.order.exception.OrderPublishingException;
import com.ordermanagement.order.exception.PublishingSuspendedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CircuitBreakingOrderEventPublisherTest {

    private final KafkaOrderEventPublisher kafkaPublisher = mock(KafkaOrderEventPublisher.class);
    private final CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
            .slidingWindowSize(4)
            .minimumNumberOfCalls(4)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .build());
    private CircuitBreakingOrderEventPublisher publisher;

    private final Order order = Order.create(
            new Customer("Ana Souza", "ana@example.com"),
            List.of(new OrderItem("SKU-1", "Keyboard", 1, new BigDecimal("150.00"))),
            Instant.parse("2026-10-21T12:00:00Z"));

    @BeforeEach
    void setUp() {
        publisher = new CircuitBreakingOrderEventPublisher(kafkaPublisher, registry);
    }

    @Test
    void publishesThroughKafkaWhileTheCircuitIsClosed() {
        publisher.publishOrderCreated(order);

        verify(kafkaPublisher).publishOrderCreated(order);
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void refusesOrdersWithoutCallingKafkaOnceTooManyPublishesFailed() {
        doThrow(new OrderPublishingException(order.id(), new RuntimeException("broker down")))
                .when(kafkaPublisher).publishOrderCreated(any());
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> publisher.publishOrderCreated(order))
                    .isExactlyInstanceOf(OrderPublishingException.class);
        }
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> publisher.publishOrderCreated(order))
                .isInstanceOfSatisfying(PublishingSuspendedException.class, e -> {
                    assertThat(e.getOrderId()).isEqualTo(order.id());
                    assertThat(e.getRetryAfter()).isEqualTo(Duration.ofSeconds(10));
                });
        verify(kafkaPublisher, times(4)).publishOrderCreated(any());
    }

    private CircuitBreaker circuitBreaker() {
        return registry.circuitBreaker(CircuitBreakingOrderEventPublisher.CIRCUIT_BREAKER_NAME);
    }
}
