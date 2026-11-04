package com.ordermanagement.order.api.dto;

import com.ordermanagement.order.domain.Customer;
import com.ordermanagement.order.domain.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 120) String customerName,
        @NotBlank @Email @Size(max = 254) String customerEmail,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull OrderItemRequest> items) {

    public Customer toCustomer() {
        return new Customer(customerName.strip(), customerEmail.strip());
    }

    public List<OrderItem> toItems() {
        return items.stream().map(OrderItemRequest::toOrderItem).toList();
    }
}
