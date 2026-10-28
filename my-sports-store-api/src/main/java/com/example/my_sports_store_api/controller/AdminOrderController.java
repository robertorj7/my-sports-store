package com.example.my_sports_store_api.controller;

import com.example.my_sports_store_api.dto.OrderStatusRequest;
import com.example.my_sports_store_api.model.Order;
import com.example.my_sports_store_api.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public List<Order> findAll() {
        return orderService.findAll();
    }

    @PatchMapping("/{id}/status")
    public Order updateStatus(@PathVariable String id, @Valid @RequestBody OrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }
}
