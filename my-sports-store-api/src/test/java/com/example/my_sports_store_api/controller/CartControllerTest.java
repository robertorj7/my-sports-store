package com.example.my_sports_store_api.controller;

import com.example.my_sports_store_api.dto.CartItemRequest;
import com.example.my_sports_store_api.dto.CartResponse;
import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.GlobalExceptionHandler;
import com.example.my_sports_store_api.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        CartController controller = new CartController(cartService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCart_returnsCurrentCart() throws Exception {
        CartResponse response = new CartResponse(List.of(), BigDecimal.ZERO);
        when(cartService.getCart()).thenReturn(response);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void addOrUpdateItem_withValidBody_returnsUpdatedCart() throws Exception {
        CartItemRequest request = new CartItemRequest("p1", 2);
        CartResponse.CartLineItem lineItem = new CartResponse.CartLineItem(
                "p1", "Ball", "img.png", BigDecimal.TEN, 2, BigDecimal.valueOf(20));
        CartResponse response = new CartResponse(List.of(lineItem), BigDecimal.valueOf(20));
        when(cartService.addOrUpdateItem(any(CartItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/cart/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value("p1"))
                .andExpect(jsonPath("$.total").value(20));
    }

    @Test
    void addOrUpdateItem_withInvalidBody_returns400() throws Exception {
        CartItemRequest invalidRequest = new CartItemRequest("", 0);

        mockMvc.perform(post("/api/cart/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addOrUpdateItem_whenInsufficientStock_returns400() throws Exception {
        CartItemRequest request = new CartItemRequest("p1", 100);
        when(cartService.addOrUpdateItem(any(CartItemRequest.class)))
                .thenThrow(new BadRequestException("Not enough stock for Ball"));

        mockMvc.perform(post("/api/cart/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not enough stock for Ball"));
    }

    @Test
    void removeItem_returnsUpdatedCart() throws Exception {
        CartResponse response = new CartResponse(List.of(), BigDecimal.ZERO);
        when(cartService.removeItem("p1")).thenReturn(response);

        mockMvc.perform(delete("/api/cart/items/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void clearCart_returnsEmptyCart() throws Exception {
        CartResponse response = new CartResponse(List.of(), BigDecimal.ZERO);
        when(cartService.clearCart()).thenReturn(response);

        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
