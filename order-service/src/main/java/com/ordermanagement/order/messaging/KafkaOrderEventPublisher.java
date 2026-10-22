package com.ordermanagement.order.messaging;

import com.ordermanagement.order.config.KafkaAppProperties;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.exception.OrderPublishingException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String topic;
    private final Duration sendTimeout;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    public KafkaOrderEventPublisher(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
                                    KafkaAppProperties properties,
                                    MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.ordersTopic().name();
        this.sendTimeout = properties.sendTimeout();
        this.publishedCounter = Counter.builder("orders.published")
                .description("Orders successfully published to Kafka")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("orders.publish.failures")
                .description("Orders that could not be published to Kafka")
                .register(meterRegistry);
    }

    @Override
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        try {
            // The order id is the record key, so every event of the same order lands on the same partition, in order
            RecordMetadata metadata = kafkaTemplate.send(topic, event.orderId(), event)
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .getRecordMetadata();
            publishedCounter.increment();
            log.info("Order {} published to topic '{}' (partition={}, offset={})",
                    event.orderId(), metadata.topic(), metadata.partition(), metadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw publishingFailed(event, e);
        } catch (ExecutionException e) {
            throw publishingFailed(event, e.getCause());
        } catch (TimeoutException | KafkaException | org.apache.kafka.common.KafkaException e) {
            throw publishingFailed(event, e);
        }
    }

    private OrderPublishingException publishingFailed(OrderCreatedEvent event, Throwable cause) {
        failedCounter.increment();
        return new OrderPublishingException(event.orderId(), cause);
    }
}
