package com.ordermanagement.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordermanagement.order.messaging.OrderCreatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Kafka producer setup: orders are published as JSON, keyed by order id, to the orders topic.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, OrderCreatedEvent> orderProducerFactory(KafkaProperties kafkaProperties,
                                                                           ObjectMapper objectMapper) {
        // Reusing Spring's ObjectMapper writes dates as ISO-8601 strings, readable by any consumer
        JsonSerializer<OrderCreatedEvent> valueSerializer = new JsonSerializer<>(objectMapper);
        // Consumers bind the payload to their own classes, so Java type headers would only leak our class names
        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties(null), new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, OrderCreatedEvent> orderKafkaTemplate(
            ProducerFactory<String, OrderCreatedEvent> orderProducerFactory) {
        return new KafkaTemplate<>(orderProducerFactory);
    }

    /** Creates the orders topic on startup if it does not exist yet. */
    @Bean
    public NewTopic ordersTopic(KafkaAppProperties properties) {
        KafkaAppProperties.OrdersTopic topic = properties.ordersTopic();
        return TopicBuilder.name(topic.name())
                .partitions(topic.partitions())
                .replicas(topic.replicationFactor())
                .build();
    }
}
