package com.ordermanagement.processing.service;

import com.ordermanagement.processing.domain.Order;
import com.ordermanagement.processing.domain.OrderStatus;
import com.ordermanagement.processing.exception.InvalidOrderEventException;
import com.ordermanagement.processing.messaging.OrderCreatedEvent;
import com.ordermanagement.processing.messaging.OrderEventValidator;
import com.ordermanagement.processing.repository.OrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderProcessingServiceTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private OrderProcessingService processingService;

    @BeforeEach
    void setUp() {
        processingService = new OrderProcessingService(
                orderRepository, new OrderEventValidator(VALIDATOR_FACTORY.getValidator()), meterRegistry);
    }

    @AfterAll
    static void closeValidator() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void storesTheOrderWithStatusCreated() {
        OrderCreatedEvent event = validEvent();

        processingService.process(event);

        ArgumentCaptor<Order> stored = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).insert(stored.capture());
        assertThat(stored.getValue().getId()).isEqualTo("order-1");
        assertThat(stored.getValue().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(stored.getValue().getItems()).hasSize(1);
        assertThat(stored.getValue().getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(meterRegistry.counter("orders.stored").count()).isEqualTo(1);
    }

    @Test
    void ignoresAnOrderThatWasAlreadyStored() {
        when(orderRepository.insert(any(Order.class))).thenThrow(new DuplicateKeyException("E11000"));

        assertThatCode(() -> processingService.process(validEvent())).doesNotThrowAnyException();
        assertThat(meterRegistry.counter("orders.duplicates").count()).isEqualTo(1);
    }

    @Test
    void rejectsAnEventWithoutItems() {
        OrderCreatedEvent event = new OrderCreatedEvent("order-1", "Ana Souza", "ana@example.com",
                List.of(), new BigDecimal("300.00"), Instant.now());

        assertThatThrownBy(() -> processingService.process(event))
                .isInstanceOf(InvalidOrderEventException.class)
                .hasMessageContaining("items");
        verify(orderRepository, never()).insert(any(Order.class));
    }

    private static OrderCreatedEvent validEvent() {
        return new OrderCreatedEvent("order-1", "Ana Souza", "ana@example.com",
                List.of(new OrderCreatedEvent.Item("SKU-1", "Keyboard", 2, new BigDecimal("150.00"))),
                new BigDecimal("300.00"), Instant.parse("2026-10-21T12:00:00Z"));
    }
}
