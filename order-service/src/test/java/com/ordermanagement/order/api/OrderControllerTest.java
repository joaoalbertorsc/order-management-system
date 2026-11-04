package com.ordermanagement.order.api;

import com.ordermanagement.order.domain.Customer;
import com.ordermanagement.order.domain.Order;
import com.ordermanagement.order.domain.OrderItem;
import com.ordermanagement.order.exception.InvalidOrderException;
import com.ordermanagement.order.exception.OrderPublishingException;
import com.ordermanagement.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String VALID_ORDER = """
            {
              "customerName": "Ana Souza",
              "customerEmail": "ana@example.com",
              "items": [
                { "productId": "SKU-1", "productName": "Keyboard", "quantity": 2, "unitPrice": 150.00 }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void returns202WithTheCreatedOrder() throws Exception {
        Order order = Order.create(
                new Customer("Ana Souza", "ana@example.com"),
                List.of(new OrderItem("SKU-1", "Keyboard", 2, new BigDecimal("150.00"))),
                Instant.parse("2026-10-21T12:00:00Z"));
        when(orderService.createOrder(any(), anyList())).thenReturn(order);

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(order.id()))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(300.00))
                .andExpect(jsonPath("$.createdAt").value("2026-10-21T12:00:00Z"));
    }

    @Test
    void returns400WithFieldErrorsForAnInvalidPayload() throws Exception {
        String invalidOrder = """
                {
                  "customerName": "",
                  "customerEmail": "not-an-email",
                  "items": [ { "productId": "SKU-1", "productName": "Keyboard", "quantity": 0, "unitPrice": 10 } ]
                }
                """;

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(invalidOrder))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.customerName").exists())
                .andExpect(jsonPath("$.errors.customerEmail").exists())
                .andExpect(jsonPath("$['errors']['items[0].quantity']").exists());
    }

    @Test
    void returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns422WhenABusinessRuleRejectsTheOrder() throws Exception {
        when(orderService.createOrder(any(), anyList())).thenThrow(new InvalidOrderException("Too expensive"));

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid order"))
                .andExpect(jsonPath("$.detail").value("Too expensive"));
    }

    @Test
    void returns503WhenKafkaIsUnavailable() throws Exception {
        when(orderService.createOrder(any(), anyList()))
                .thenThrow(new OrderPublishingException("order-1", new RuntimeException("broker down")));

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Messaging unavailable"))
                .andExpect(jsonPath("$.orderId").value("order-1"));
    }
}
