package com.ordermanagement.print.printer;

import com.ordermanagement.print.messaging.OrderCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderReceiptFormatterTest {

    private final OrderReceiptFormatter formatter = new OrderReceiptFormatter();

    @Test
    void rendersEveryOrderDetail() {
        OrderCreatedEvent order = new OrderCreatedEvent(
                "order-1", "Ana Souza", "ana@example.com",
                List.of(
                        new OrderCreatedEvent.Item("SKU-1", "Keyboard", 2, new BigDecimal("150.00")),
                        new OrderCreatedEvent.Item("SKU-2", "Mouse", 1, new BigDecimal("79.90"))),
                new BigDecimal("379.90"),
                Instant.parse("2026-10-21T12:00:00Z"));

        String receipt = formatter.format(order);

        assertThat(receipt)
                .contains("Order      : order-1")
                .contains("Customer   : Ana Souza <ana@example.com>")
                .contains("Created at : 2026-10-21T12:00:00Z")
                .contains("2 x Keyboard (SKU-1) @ 150.00 = 300.00")
                .contains("1 x Mouse (SKU-2) @ 79.90 = 79.90")
                .contains("Total      : 379.90");
    }

    @Test
    void stillRendersAnIncompleteOrder() {
        OrderCreatedEvent order = new OrderCreatedEvent("order-2", null, null, null, null, null);

        String receipt = formatter.format(order);

        assertThat(receipt)
                .contains("Order      : order-2")
                .contains("Customer   : - <->")
                .contains("(no items)")
                .contains("Total      : -");
    }
}
