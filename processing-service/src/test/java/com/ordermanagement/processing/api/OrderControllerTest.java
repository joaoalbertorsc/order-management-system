package com.ordermanagement.processing.api;

import com.ordermanagement.processing.domain.Order;
import com.ordermanagement.processing.domain.OrderItem;
import com.ordermanagement.processing.domain.OrderStatus;
import com.ordermanagement.processing.exception.InvalidStatusTransitionException;
import com.ordermanagement.processing.exception.OrderNotFoundException;
import com.ordermanagement.processing.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final Order order = new Order("order-1", "Ana Souza", "ana@example.com",
            List.of(new OrderItem("SKU-1", "Keyboard", 2, new BigDecimal("150.00"))),
            new BigDecimal("300.00"), Instant.parse("2026-10-21T12:00:00Z"));

    @Test
    void listsOrdersFilteredByStatus() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(orderService.findOrders(eq(OrderStatus.CREATED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        mockMvc.perform(get("/orders").param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("order-1"))
                .andExpect(jsonPath("$.content[0].status").value("CREATED"))
                .andExpect(jsonPath("$.content[0].items[0].subtotal").value(300.00))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void rejectsAnUnknownStatusFilter() throws Exception {
        mockMvc.perform(get("/orders").param("status", "LOST"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForAnUnknownOrder() throws Exception {
        when(orderService.findOrder("missing")).thenThrow(new OrderNotFoundException("missing"));

        mockMvc.perform(get("/orders/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"));
    }

    @Test
    void updatesTheStatus() throws Exception {
        order.changeStatus(OrderStatus.CONFIRMED, Instant.parse("2026-10-21T13:00:00Z"));
        when(orderService.updateStatus("order-1", OrderStatus.CONFIRMED)).thenReturn(order);

        mockMvc.perform(patch("/orders/order-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-21T13:00:00Z"));
    }

    @Test
    void returns409ForAForbiddenTransition() throws Exception {
        when(orderService.updateStatus("order-1", OrderStatus.DELIVERED)).thenThrow(
                new InvalidStatusTransitionException("order-1", OrderStatus.CREATED, OrderStatus.DELIVERED));

        mockMvc.perform(patch("/orders/order-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("CREATED"))
                .andExpect(jsonPath("$.allowedStatuses[0]").value("CONFIRMED"))
                .andExpect(jsonPath("$.allowedStatuses[1]").value("CANCELLED"));
    }

    @Test
    void listsTheAcceptedValuesForAnUnknownStatus() throws Exception {
        mockMvc.perform(patch("/orders/order-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"LOST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Invalid value 'LOST'. Accepted values: [CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED]"));
    }

    @Test
    void returns400WhenTheStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/orders/order-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
    }
}
