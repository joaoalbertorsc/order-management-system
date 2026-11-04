package com.ordermanagement.print.messaging;

import com.ordermanagement.print.printer.OrderPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final OrderPrinter orderPrinter;

    public OrderCreatedListener(OrderPrinter orderPrinter) {
        this.orderPrinter = orderPrinter;
    }

    @KafkaListener(topics = "${app.kafka.orders-topic}")
    public void onOrderCreated(@Payload OrderCreatedEvent event,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received order {} (partition={}, offset={})", event.orderId(), partition, offset);
        orderPrinter.print(event);
    }
}
