package com.ordermanagement.order.validation;

import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.exception.InvalidOrderException;

/**
 * A single business rule checked before an order is published.
 * New rules are added as new beans, without touching the service that runs them.
 */
public interface OrderValidationRule {

    /**
     * @throws InvalidOrderException when the order breaks this rule
     */
    void validate(Order order);
}
