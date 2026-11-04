package com.ordermanagement.processing.service;

import com.ordermanagement.processing.domain.Order;
import com.ordermanagement.processing.domain.OrderItem;
import com.ordermanagement.processing.domain.OrderStatus;
import com.ordermanagement.processing.exception.OrderNotFoundException;
import com.ordermanagement.processing.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderService orderService = new OrderService(orderRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void listsAllOrdersWhenNoStatusIsGiven() {
        Pageable pageable = PageRequest.of(0, 20);

        orderService.findOrders(null, pageable);

        verify(orderRepository).findAll(pageable);
    }

    @Test
    void filtersOrdersByStatus() {
        Pageable pageable = PageRequest.of(0, 20);

        orderService.findOrders(OrderStatus.SHIPPED, pageable);

        verify(orderRepository).findByStatus(OrderStatus.SHIPPED, pageable);
    }

    @Test
    void failsWhenTheOrderDoesNotExist() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findOrder("missing"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void updatesTheStatusAndSavesTheOrder() {
        Order order = new Order("order-1", "Ana Souza", "ana@example.com",
                List.of(new OrderItem("SKU-1", "Keyboard", 1, BigDecimal.TEN)), BigDecimal.TEN, NOW.minusSeconds(3600));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.updateStatus("order-1", OrderStatus.CONFIRMED);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
        verify(orderRepository).save(order);
    }
}
