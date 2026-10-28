package com.example.my_sports_store_api.dto;

import com.example.my_sports_store_api.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @NotNull OrderStatus status
) {
}
