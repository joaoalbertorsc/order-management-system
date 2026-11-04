package com.ordermanagement.order.api.dto;

import com.ordermanagement.order.domain.OrderItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank @Size(max = 64) String productId,
        @NotBlank @Size(max = 120) String productName,
        @NotNull @Positive @Max(1000) Integer quantity,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {

    public OrderItem toOrderItem() {
        return new OrderItem(productId.strip(), productName.strip(), quantity, unitPrice);
    }
}
