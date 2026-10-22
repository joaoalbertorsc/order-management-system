package com.ordermanagement.processing.domain;

import com.ordermanagement.processing.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-21T12:00:00Z");

    @ParameterizedTest
    @CsvSource({
            "CREATED, CONFIRMED, true",
            "CREATED, CANCELLED, true",
            "CREATED, SHIPPED, false",
            "CONFIRMED, SHIPPED, true",
            "CONFIRMED, CANCELLED, true",
            "SHIPPED, DELIVERED, true",
            "SHIPPED, CANCELLED, false",
            "DELIVERED, CREATED, false",
            "CANCELLED, CONFIRMED, false",
            "CREATED, CREATED, false"
    })
    void followsTheOrderLifecycle(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void newOrdersStartAsCreated() {
        Order order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void changesStatusAndRecordsWhen() {
        Order order = newOrder();
        Instant changedAt = CREATED_AT.plusSeconds(60);

        order.changeStatus(OrderStatus.CONFIRMED, changedAt);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getUpdatedAt()).isEqualTo(changedAt);
    }

    @Test
    void rejectsTransitionsOutsideTheLifecycle() {
        Order order = newOrder();

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.DELIVERED, Instant.now()))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CREATED to DELIVERED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    private static Order newOrder() {
        return new Order("order-1", "Ana Souza", "ana@example.com",
                List.of(new OrderItem("SKU-1", "Keyboard", 1, BigDecimal.TEN)), BigDecimal.TEN, CREATED_AT);
    }
}
