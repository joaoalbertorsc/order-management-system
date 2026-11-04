package com.ordermanagement.processing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordermanagement.processing.exception.InvalidOrderEventException;
import com.ordermanagement.processing.messaging.OrderCreatedEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer setup: JSON records from the orders topic become {@link OrderCreatedEvent}s.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory(KafkaProperties kafkaProperties,
                                                                           ObjectMapper objectMapper) {
        // Always bind to our own event class instead of trusting type headers sent by the producer
        JsonDeserializer<OrderCreatedEvent> jsonDeserializer =
                new JsonDeserializer<>(OrderCreatedEvent.class, objectMapper, false);

        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                // A malformed record becomes an error the handler can skip, instead of failing every poll forever
                new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory, DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    /**
     * Retries a failed record 3 times, 1 second apart (e.g. MongoDB briefly down), then logs it and moves on.
     * Records that can never succeed (malformed JSON, invalid order data) are skipped without retrying.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error("Skipping record from {}-{} at offset {}: {}",
                        record.topic(), record.partition(), record.offset(),
                        NestedExceptionUtils.getMostSpecificCause(exception).getMessage()),
                new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(InvalidOrderEventException.class);
        return errorHandler;
    }
}
