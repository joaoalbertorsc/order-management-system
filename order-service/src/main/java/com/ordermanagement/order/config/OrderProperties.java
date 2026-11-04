package com.ordermanagement.order.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Business limits applied when a new order is received.
 *
 * @param maxTotalAmount highest total accepted for a single order
 */
@Validated
@ConfigurationProperties(prefix = "app.orders")
public record OrderProperties(@NotNull @Positive BigDecimal maxTotalAmount) {
}
