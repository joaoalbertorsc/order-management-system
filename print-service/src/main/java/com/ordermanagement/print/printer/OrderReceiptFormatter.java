package com.ordermanagement.print.printer;

import com.ordermanagement.print.messaging.OrderCreatedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Renders an order as a plain-text receipt. Missing fields are shown as "-" so that an incomplete
 * message is still printed instead of failing.
 */
@Component
public class OrderReceiptFormatter {

    private static final String RULE = "=".repeat(60);
    private static final String MISSING = "-";

    public String format(OrderCreatedEvent order) {
        StringBuilder receipt = new StringBuilder()
                .append(RULE).append('\n')
                .append("  NEW ORDER RECEIVED").append('\n')
                .append(RULE).append('\n')
                .append(line("Order", order.orderId()))
                .append(line("Customer", "%s <%s>".formatted(text(order.customerName()), text(order.customerEmail()))))
                .append(line("Created at", order.createdAt()))
                .append("  Items      :\n");

        List<OrderCreatedEvent.Item> items = Objects.requireNonNullElse(order.items(), List.of());
        if (items.isEmpty()) {
            receipt.append("    (no items)\n");
        }
        for (OrderCreatedEvent.Item item : items) {
            receipt.append("    %d x %s (%s) @ %s = %s\n".formatted(
                    item.quantity(),
                    text(item.productName()),
                    text(item.productId()),
                    amount(item.unitPrice()),
                    amount(subtotal(item))));
        }

        return receipt
                .append(line("Total", amount(order.totalAmount())))
                .append(RULE)
                .toString();
    }

    private static String line(String label, Object value) {
        return "  %-11s: %s\n".formatted(label, text(value));
    }

    private static String text(Object value) {
        return Objects.toString(value, MISSING);
    }

    private static String amount(BigDecimal value) {
        return value == null ? MISSING : value.toPlainString();
    }

    private static BigDecimal subtotal(OrderCreatedEvent.Item item) {
        return item.unitPrice() == null ? null : item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
    }
}
