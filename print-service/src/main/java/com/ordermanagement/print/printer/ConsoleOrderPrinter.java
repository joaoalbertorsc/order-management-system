package com.ordermanagement.print.printer;

import com.ordermanagement.print.messaging.OrderCreatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Prints each order to the console through the application log, so receipts carry a timestamp
 * and end up wherever the logs are collected (e.g. {@code docker compose logs print-service}).
 */
@Component
public class ConsoleOrderPrinter implements OrderPrinter {

    private static final Logger log = LoggerFactory.getLogger(ConsoleOrderPrinter.class);

    private final OrderReceiptFormatter formatter;
    private final Counter printedCounter;

    public ConsoleOrderPrinter(OrderReceiptFormatter formatter, MeterRegistry meterRegistry) {
        this.formatter = formatter;
        this.printedCounter = Counter.builder("orders.printed")
                .description("Orders printed to the console")
                .register(meterRegistry);
    }

    @Override
    public void print(OrderCreatedEvent order) {
        log.info("Processing order {}\n{}", order.orderId(), formatter.format(order));
        printedCounter.increment();
    }
}
