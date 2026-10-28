package com.example.my_sports_store_api.event;

import com.example.my_sports_store_api.model.OrderStatus;

import java.time.Instant;

public record OrderStatusChangedEvent(
        String orderId,
        String userId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant changedAt
) {
}
