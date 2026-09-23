package com.example.my_sports_store_api.controller;

import com.example.my_sports_store_api.model.Order;
import com.example.my_sports_store_api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout() {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout());
    }

    @GetMapping
    public List<Order> findMyOrders() {
        return orderService.findMyOrders();
    }

    @GetMapping("/{id}")
    public Order findMyOrder(@PathVariable String id) {
        return orderService.findMyOrder(id);
    }
}
