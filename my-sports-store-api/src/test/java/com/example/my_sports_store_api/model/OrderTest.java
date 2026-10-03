package com.example.my_sports_store_api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void newOrder_hasEmptyMutableItemsList() {
        Order order = new Order();

        assertThat(order.getItems()).isNotNull().isEmpty();

        order.getItems().add(new OrderItem("product-1", "Ball", null, 1));
        assertThat(order.getItems()).hasSize(1);
    }
}
