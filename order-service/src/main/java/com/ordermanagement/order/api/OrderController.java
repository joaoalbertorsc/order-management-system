package com.ordermanagement.order.api;

import com.ordermanagement.order.api.dto.CreateOrderRequest;
import com.ordermanagement.order.api.dto.OrderResponse;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Accepts a new order. Returns 202 because the order is persisted asynchronously by the processing-service.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request.toCustomer(), request.toItems());
        return OrderResponse.from(order);
    }
}
