package com.example.my_sports_store_api.controller;

import com.example.my_sports_store_api.dto.CartItemRequest;
import com.example.my_sports_store_api.dto.CartResponse;
import com.example.my_sports_store_api.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart() {
        return cartService.getCart();
    }

    @PostMapping("/items")
    public CartResponse addOrUpdateItem(@Valid @RequestBody CartItemRequest request) {
        return cartService.addOrUpdateItem(request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable String productId) {
        return cartService.removeItem(productId);
    }

    @DeleteMapping
    public CartResponse clearCart() {
        return cartService.clearCart();
    }
}
