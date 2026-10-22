package com.ordermanagement.order.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Kafka settings owned by this application (everything under {@code app.kafka}).
 *
 * @param sendTimeout maximum time an HTTP request waits for the broker to acknowledge an order
 * @param ordersTopic topic where new orders are published
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaAppProperties(@NotNull Duration sendTimeout, @Valid @NotNull OrdersTopic ordersTopic) {

    public record OrdersTopic(@NotBlank String name, @Positive int partitions, @Positive int replicationFactor) {
    }
}
