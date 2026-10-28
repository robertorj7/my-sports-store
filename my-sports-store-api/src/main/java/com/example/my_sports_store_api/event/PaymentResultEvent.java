package com.example.my_sports_store_api.event;

public record PaymentResultEvent(
        String orderId,
        boolean approved,
        String reason
) {
}
