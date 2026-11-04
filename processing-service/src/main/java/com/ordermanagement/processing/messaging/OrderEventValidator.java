package com.ordermanagement.processing.messaging;

import com.ordermanagement.processing.exception.InvalidOrderEventException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks the constraints declared on {@link OrderCreatedEvent}. Kafka messages skip the
 * validation Spring MVC does for HTTP requests, so this consumer has to check them itself.
 */
@Component
public class OrderEventValidator {

    private final Validator validator;

    public OrderEventValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * @throws InvalidOrderEventException listing every broken constraint
     */
    public void validate(OrderCreatedEvent event) {
        Set<ConstraintViolation<OrderCreatedEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new InvalidOrderEventException(event.orderId(), details);
        }
    }
}
