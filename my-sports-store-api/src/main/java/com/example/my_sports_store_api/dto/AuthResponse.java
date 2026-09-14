package com.example.my_sports_store_api.dto;

import java.util.Set;

public record AuthResponse(
        String token,
        String email,
        String name,
        Set<String> roles
) {
}
