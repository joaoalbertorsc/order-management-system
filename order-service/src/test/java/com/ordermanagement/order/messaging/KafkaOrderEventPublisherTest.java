package com.ordermanagement.order.messaging;

import com.ordermanagement.order.config.KafkaAppProperties;
import com.ordermanagement.order.domain.Customer;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.domain.OrderItem;
import com.ordermanagement.order.exception.OrderPublishingException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaOrderEventPublisherTest {

    private static final String TOPIC = "pedidos";

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate = mock(KafkaTemplate.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private KafkaOrderEventPublisher publisher;

    private final Order order = Order.create(
            new Customer("Ana Souza", "ana@example.com"),
            List.of(new OrderItem("SKU-1", "Keyboard", 1, new BigDecimal("150.00"))),
            Instant.parse("2026-10-21T12:00:00Z"));

    @BeforeEach
    void setUp() {
        KafkaAppProperties properties = new KafkaAppProperties(
                Duration.ofSeconds(1), new KafkaAppProperties.OrdersTopic(TOPIC, 3, 1));
        publisher = new KafkaOrderEventPublisher(kafkaTemplate, properties, meterRegistry);
    }

    @Test
    void publishesTheOrderKeyedByItsId() {
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 0), 0, 0, 0, 0, 0);
        SendResult<String, OrderCreatedEvent> result = new SendResult<>(
                new ProducerRecord<>(TOPIC, order.id(), OrderCreatedEvent.from(order)), metadata);
        when(kafkaTemplate.send(eq(TOPIC), eq(order.id()), any())).thenReturn(CompletableFuture.completedFuture(result));

        publisher.publishOrderCreated(order);

        verify(kafkaTemplate).send(TOPIC, order.id(), OrderCreatedEvent.from(order));
        assertThat(meterRegistry.counter("orders.published").count()).isEqualTo(1);
    }

    @Test
    void wrapsBrokerFailuresInOrderPublishingException() {
        when(kafkaTemplate.send(eq(TOPIC), eq(order.id()), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("broker unavailable")));

        assertThatThrownBy(() -> publisher.publishOrderCreated(order))
                .isInstanceOf(OrderPublishingException.class)
                .hasRootCauseMessage("broker unavailable");
        assertThat(meterRegistry.counter("orders.publish.failures").count()).isEqualTo(1);
    }

    @Test
    void failsWhenTheBrokerDoesNotAnswerInTime() {
        when(kafkaTemplate.send(eq(TOPIC), eq(order.id()), any())).thenReturn(new CompletableFuture<>());

        assertThatThrownBy(() -> publisher.publishOrderCreated(order))
                .isInstanceOf(OrderPublishingException.class)
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
    }
}
