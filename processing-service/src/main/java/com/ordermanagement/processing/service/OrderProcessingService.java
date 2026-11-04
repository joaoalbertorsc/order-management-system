package com.ordermanagement.processing.service;

import com.ordermanagement.processing.domain.Order;
import com.ordermanagement.processing.domain.OrderItem;
import com.ordermanagement.processing.messaging.OrderCreatedEvent;
import com.ordermanagement.processing.messaging.OrderEventValidator;
import com.ordermanagement.processing.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stores the orders received from Kafka.
 */
@Service
public class OrderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    private final OrderRepository orderRepository;
    private final OrderEventValidator eventValidator;
    private final Counter storedCounter;
    private final Counter duplicateCounter;

    public OrderProcessingService(OrderRepository orderRepository,
                                  OrderEventValidator eventValidator,
                                  MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.eventValidator = eventValidator;
        this.storedCounter = Counter.builder("orders.stored")
                .description("Orders stored in MongoDB")
                .register(meterRegistry);
        this.duplicateCounter = Counter.builder("orders.duplicates")
                .description("Redelivered orders that were already stored")
                .register(meterRegistry);
    }

    public void process(OrderCreatedEvent event) {
        eventValidator.validate(event);
        try {
            orderRepository.insert(toOrder(event));
            storedCounter.increment();
            log.info("Order {} stored in MongoDB", event.orderId());
        } catch (DuplicateKeyException e) {
            // Kafka guarantees at-least-once delivery, so the same order may arrive twice; the first copy wins
            duplicateCounter.increment();
            log.warn("Order {} was already stored; ignoring duplicate delivery", event.orderId());
        }
    }

    private static Order toOrder(OrderCreatedEvent event) {
        List<OrderItem> items = event.items().stream()
                .map(item -> new OrderItem(item.productId(), item.productName(), item.quantity(), item.unitPrice()))
                .toList();
        return new Order(
                event.orderId(),
                event.customerName(),
                event.customerEmail(),
                items,
                event.totalAmount(),
                event.createdAt());
    }
}
