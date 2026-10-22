package com.ordermanagement.processing.api;

import com.ordermanagement.processing.api.dto.OrderResponse;
import com.ordermanagement.processing.api.dto.PageResponse;
import com.ordermanagement.processing.api.dto.UpdateOrderStatusRequest;
import com.ordermanagement.processing.domain.OrderStatus;
import com.ordermanagement.processing.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read side of the system: orders become visible here once this service has consumed them from Kafka.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public PageResponse<OrderResponse> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(orderService.findOrders(status, pageable), OrderResponse::from);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return OrderResponse.from(orderService.findOrder(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(@PathVariable String orderId,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return OrderResponse.from(orderService.updateStatus(orderId, request.status()));
    }
}
