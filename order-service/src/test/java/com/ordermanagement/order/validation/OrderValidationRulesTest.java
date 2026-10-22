package com.ordermanagement.order.validation;

import com.ordermanagement.order.config.OrderProperties;
import com.ordermanagement.order.domain.Customer;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.domain.OrderItem;
import com.ordermanagement.order.exception.InvalidOrderException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderValidationRulesTest {

    private static final Customer CUSTOMER = new Customer("Ana Souza", "ana@example.com");

    private final UniqueProductRule uniqueProductRule = new UniqueProductRule();
    private final MaxTotalAmountRule maxTotalAmountRule =
            new MaxTotalAmountRule(new OrderProperties(new BigDecimal("1000.00")));

    @Test
    void uniqueProductRuleRejectsRepeatedProducts() {
        Order order = orderOf(
                new OrderItem("SKU-1", "Keyboard", 1, BigDecimal.TEN),
                new OrderItem("SKU-1", "Keyboard", 2, BigDecimal.TEN));

        assertThatThrownBy(() -> uniqueProductRule.validate(order))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("SKU-1");
    }

    @Test
    void uniqueProductRuleAcceptsDistinctProducts() {
        Order order = orderOf(
                new OrderItem("SKU-1", "Keyboard", 1, BigDecimal.TEN),
                new OrderItem("SKU-2", "Mouse", 1, BigDecimal.TEN));

        assertThatCode(() -> uniqueProductRule.validate(order)).doesNotThrowAnyException();
    }

    @Test
    void maxTotalAmountRuleRejectsOrdersAboveTheLimit() {
        Order order = orderOf(new OrderItem("SKU-1", "Monitor", 2, new BigDecimal("500.01")));

        assertThatThrownBy(() -> maxTotalAmountRule.validate(order))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("1000.02");
    }

    @Test
    void maxTotalAmountRuleAcceptsOrdersAtTheLimit() {
        Order order = orderOf(new OrderItem("SKU-1", "Monitor", 2, new BigDecimal("500.00")));

        assertThatCode(() -> maxTotalAmountRule.validate(order)).doesNotThrowAnyException();
    }

    private static Order orderOf(OrderItem... items) {
        return Order.create(CUSTOMER, List.of(items), Instant.now());
    }
}
