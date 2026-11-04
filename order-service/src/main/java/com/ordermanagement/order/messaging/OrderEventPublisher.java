package com.ordermanagement.order.messaging;

import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.exception.OrderPublishingException;

/**
 * Port used by the service layer to announce new orders, keeping it unaware of the message broker in use.
 */
public interface OrderEventPublisher {

    /**
     * Publishes the order and waits for the broker to acknowledge it.
     *
     * @throws OrderPublishingException when the broker does not confirm the message
     */
    void publishOrderCreated(Order order);
}
