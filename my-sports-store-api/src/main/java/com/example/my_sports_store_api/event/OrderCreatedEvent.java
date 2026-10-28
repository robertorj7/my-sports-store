package com.example.my_sports_store_api.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        BigDecimal total,
        Instant createdAt
) {
}
