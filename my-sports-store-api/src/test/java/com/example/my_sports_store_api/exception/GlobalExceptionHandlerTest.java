package com.example.my_sports_store_api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404WithMessage() {
        ResponseEntity<ApiError> response = handler.handleNotFound(new ResourceNotFoundException("Product not found: 1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Product not found: 1");
    }

    @Test
    void handleBadRequest_returns400WithMessage() {
        ResponseEntity<ApiError> response = handler.handleBadRequest(new BadRequestException("Cannot checkout an empty cart"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Cannot checkout an empty cart");
    }

    @Test
    void handleBadCredentials_returns401WithGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleBadCredentials(new BadCredentialsException("bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
    }
}
