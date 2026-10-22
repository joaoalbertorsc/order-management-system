package com.ordermanagement.processing.messaging;

import com.ordermanagement.processing.service.OrderProcessingService;
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

    private final OrderProcessingService processingService;

    public OrderCreatedListener(OrderProcessingService processingService) {
        this.processingService = processingService;
    }

    @KafkaListener(topics = "${app.kafka.orders-topic}")
    public void onOrderCreated(@Payload OrderCreatedEvent event,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received order {} (partition={}, offset={})", event.orderId(), partition, offset);
        processingService.process(event);
    }
}
