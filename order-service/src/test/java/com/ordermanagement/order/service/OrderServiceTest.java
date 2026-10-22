package com.ordermanagement.order.service;

import com.ordermanagement.order.domain.Customer;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.domain.OrderItem;
import com.ordermanagement.order.exception.InvalidOrderException;
import com.ordermanagement.order.messaging.OrderEventPublisher;
import com.ordermanagement.order.validation.OrderValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");
    private static final Customer CUSTOMER = new Customer("Ana Souza", "ana@example.com");

    private final OrderValidationRule rule = mock(OrderValidationRule.class);
    private final OrderEventPublisher publisher = mock(OrderEventPublisher.class);
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(List.of(rule), publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsOrderWithComputedTotalAndPublishesIt() {
        List<OrderItem> items = List.of(
                new OrderItem("SKU-1", "Keyboard", 2, new BigDecimal("150.00")),
                new OrderItem("SKU-2", "Mouse", 1, new BigDecimal("79.90")));

        Order order = orderService.createOrder(CUSTOMER, items);

        assertThat(order.id()).isNotBlank();
        assertThat(order.totalAmount()).isEqualByComparingTo("379.90");
        assertThat(order.createdAt()).isEqualTo(NOW);
        verify(rule).validate(order);
        verify(publisher).publishOrderCreated(order);
    }

    @Test
    void doesNotPublishWhenARuleRejectsTheOrder() {
        doThrow(new InvalidOrderException("rejected")).when(rule).validate(any());
        List<OrderItem> items = List.of(new OrderItem("SKU-1", "Keyboard", 1, BigDecimal.TEN));

        assertThatThrownBy(() -> orderService.createOrder(CUSTOMER, items))
                .isInstanceOf(InvalidOrderException.class);
        verify(publisher, never()).publishOrderCreated(any());
    }
}
