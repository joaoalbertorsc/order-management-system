package com.ordermanagement.print.printer;

import com.ordermanagement.print.messaging.OrderCreatedEvent;

/**
 * Output channel for received orders. The console is the only one today; a file or
 * thermal-printer implementation could be added without changing the Kafka listener.
 */
public interface OrderPrinter {

    void print(OrderCreatedEvent order);
}
