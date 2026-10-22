package com.ordermanagement.order.validation;

import com.ordermanagement.order.config.OrderProperties;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.exception.InvalidOrderException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Rejects orders whose total exceeds the configured limit ({@code app.orders.max-total-amount}).
 */
@Component
public class MaxTotalAmountRule implements OrderValidationRule {

    private final BigDecimal maxTotalAmount;

    public MaxTotalAmountRule(OrderProperties properties) {
        this.maxTotalAmount = properties.maxTotalAmount();
    }

    @Override
    public void validate(Order order) {
        if (order.totalAmount().compareTo(maxTotalAmount) > 0) {
            throw new InvalidOrderException(
                    "Order total %s exceeds the maximum allowed of %s"
                            .formatted(order.totalAmount().toPlainString(), maxTotalAmount.toPlainString()));
        }
    }
}
