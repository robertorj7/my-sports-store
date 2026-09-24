package com.example.my_sports_store_api.controller;

import com.example.my_sports_store_api.dto.AuthResponse;
import com.example.my_sports_store_api.dto.LoginRequest;
import com.example.my_sports_store_api.dto.RegisterRequest;
import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.GlobalExceptionHandler;
import com.example.my_sports_store_api.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_withValidBody_returns201AndAuthResponse() throws Exception {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");
        AuthResponse response = new AuthResponse("jwt-token", "new@example.com", "New User", Set.of("ROLE_USER"));
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void register_withInvalidBody_returns400() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("not-an-email", "123", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_whenEmailAlreadyExists_returns400WithErrorBody() throws Exception {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "Existing");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("An account with this email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void login_withValidBody_returns200AndAuthResponse() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        AuthResponse response = new AuthResponse("jwt-token", "user@example.com", "User", Set.of("ROLE_USER"));
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_whenServiceThrowsBadRequest_returns400() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadRequestException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
