package com.ordermanagement.processing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordermanagement.processing.exception.InvalidOrderEventException;
import com.ordermanagement.processing.messaging.OrderCreatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka consumer setup: JSON records from the orders topic become {@link OrderCreatedEvent}s.
 * Records that cannot be stored end up in a dead letter topic instead of being dropped.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    /** Long enough to ride out a MongoDB restart; the partition waits meanwhile, so no order is skipped. */
    private static final Duration MAX_RETRY_TIME = Duration.ofMinutes(5);
    private static final Duration MAX_RETRY_INTERVAL = Duration.ofSeconds(30);

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
     * Producer for the dead letter topic. Malformed records are written back as the raw bytes they arrived
     * with; events that were read fine but could not be stored are written as JSON again.
     */
    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate(KafkaProperties kafkaProperties,
                                                                 ObjectMapper objectMapper) {
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(objectMapper);
        jsonSerializer.setAddTypeInfo(false);
        // Checked in order, so byte[] must come before the catch-all Object
        Map<Class<?>, Serializer<?>> valueSerializers = new LinkedHashMap<>();
        valueSerializers.put(byte[].class, new ByteArraySerializer());
        valueSerializers.put(Object.class, jsonSerializer);

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties(null),
                new StringSerializer(),
                new DelegatingByTypeSerializer(valueSerializers, true)));
    }

    @Bean
    public NewTopic deadLetterTopic(@Value("${app.kafka.dead-letter-topic}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }

    /**
     * Retries a failed record with exponential backoff (1 s, 2 s, 4 s... up to 30 s apart) for up to 5 minutes,
     * e.g. while MongoDB is down, then moves it to the dead letter topic so it can be inspected and replayed.
     * Records that can never succeed (malformed JSON, invalid order data) go there without retrying.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<String, Object> deadLetterKafkaTemplate,
                                                 @Value("${app.kafka.dead-letter-topic}") String deadLetterTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate,
                (record, exception) -> {
                    log.error("Moving record from {}-{} at offset {} to '{}': {}",
                            record.topic(), record.partition(), record.offset(), deadLetterTopic,
                            NestedExceptionUtils.getMostSpecificCause(exception).getMessage());
                    // A negative partition lets the producer choose one from the key, whatever the topic size
                    return new TopicPartition(deadLetterTopic, -1);
                });

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(MAX_RETRY_INTERVAL.toMillis());
        backOff.setMaxElapsedTime(MAX_RETRY_TIME.toMillis());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(InvalidOrderEventException.class);
        return errorHandler;
    }
}
