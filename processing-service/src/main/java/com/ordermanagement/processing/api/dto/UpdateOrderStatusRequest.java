package com.ordermanagement.processing.api.dto;

import com.ordermanagement.processing.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
