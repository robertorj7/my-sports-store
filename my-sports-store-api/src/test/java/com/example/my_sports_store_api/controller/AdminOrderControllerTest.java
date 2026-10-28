package com.example.my_sports_store_api.controller;

import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.GlobalExceptionHandler;
import com.example.my_sports_store_api.model.Order;
import com.example.my_sports_store_api.model.OrderStatus;
import com.example.my_sports_store_api.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminOrderController controller = new AdminOrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Order order(String id, OrderStatus status) {
        return new Order(id, "user-1", List.of(), BigDecimal.valueOf(20), status, Instant.now());
    }

    @Test
    void findAll_returnsEveryOrder() throws Exception {
        when(orderService.findAll()).thenReturn(List.of(order("o1", OrderStatus.PAID), order("o2", OrderStatus.PENDING)));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateStatus_whenAllowed_returnsUpdatedOrder() throws Exception {
        when(orderService.updateStatus("o1", OrderStatus.SHIPPED)).thenReturn(order("o1", OrderStatus.SHIPPED));

        mockMvc.perform(patch("/api/admin/orders/o1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void updateStatus_whenTransitionIsInvalid_returns400() throws Exception {
        when(orderService.updateStatus("o1", OrderStatus.DELIVERED))
                .thenThrow(new BadRequestException("Cannot change order status from PENDING to DELIVERED"));

        mockMvc.perform(patch("/api/admin/orders/o1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot change order status from PENDING to DELIVERED"));
    }

    @Test
    void updateStatus_whenStatusIsMissing_returns400() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/o1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());

        verify(orderService, never()).updateStatus(any(), any());
    }
}
