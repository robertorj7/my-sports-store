package com.example.my_sports_store_api.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING, PAID, true",
            "PENDING, CANCELLED, true",
            "PENDING, SHIPPED, false",
            "PAID, SHIPPED, true",
            "PAID, CANCELLED, true",
            "PAID, PENDING, false",
            "SHIPPED, DELIVERED, true",
            "SHIPPED, CANCELLED, false",
            "DELIVERED, CANCELLED, false",
            "CANCELLED, PAID, false",
    })
    void canTransitionTo_followsOrderLifecycle(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }
}
