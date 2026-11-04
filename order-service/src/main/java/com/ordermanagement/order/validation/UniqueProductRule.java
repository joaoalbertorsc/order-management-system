package com.ordermanagement.order.validation;

import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.domain.OrderItem;
import com.ordermanagement.order.exception.InvalidOrderException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Each product must appear only once per order; repeated products should be sent as one item with a larger quantity.
 */
@Component
public class UniqueProductRule implements OrderValidationRule {

    @Override
    public void validate(Order order) {
        Set<String> seen = new HashSet<>();
        for (OrderItem item : order.items()) {
            if (!seen.add(item.productId())) {
                throw new InvalidOrderException(
                        "Product '%s' appears more than once; send it as a single item with the total quantity"
                                .formatted(item.productId()));
            }
        }
    }
}
