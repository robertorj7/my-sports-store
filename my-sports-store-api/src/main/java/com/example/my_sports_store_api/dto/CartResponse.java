package com.example.my_sports_store_api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartLineItem> items,
        BigDecimal total
) {
    public record CartLineItem(
            String productId,
            String name,
            String image,
            BigDecimal price,
            int quantity,
            BigDecimal lineTotal
    ) {
    }
}
