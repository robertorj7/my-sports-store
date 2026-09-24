package com.example.my_sports_store_api.controller;

import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.GlobalExceptionHandler;
import com.example.my_sports_store_api.exception.ResourceNotFoundException;
import com.example.my_sports_store_api.model.Order;
import com.example.my_sports_store_api.model.OrderStatus;
import com.example.my_sports_store_api.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Order order(String id) {
        return new Order(id, "user-1", List.of(), BigDecimal.valueOf(20), OrderStatus.PENDING, Instant.now());
    }

    @Test
    void checkout_whenSuccessful_returns201WithOrder() throws Exception {
        when(orderService.checkout()).thenReturn(order("o1"));

        mockMvc.perform(post("/api/orders/checkout"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("o1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void checkout_whenCartIsEmpty_returns400() throws Exception {
        when(orderService.checkout()).thenThrow(new BadRequestException("Cannot checkout an empty cart"));

        mockMvc.perform(post("/api/orders/checkout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot checkout an empty cart"));
    }

    @Test
    void findMyOrders_returnsOrderList() throws Exception {
        when(orderService.findMyOrders()).thenReturn(List.of(order("o1"), order("o2")));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void findMyOrder_whenFound_returnsOrder() throws Exception {
        when(orderService.findMyOrder("o1")).thenReturn(order("o1"));

        mockMvc.perform(get("/api/orders/o1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("o1"));
    }

    @Test
    void findMyOrder_whenNotFound_returns404() throws Exception {
        when(orderService.findMyOrder("missing"))
                .thenThrow(new ResourceNotFoundException("Order not found: missing"));

        mockMvc.perform(get("/api/orders/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: missing"));
    }
}
