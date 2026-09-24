package com.example.my_sports_store_api.service;

import com.example.my_sports_store_api.dto.AuthResponse;
import com.example.my_sports_store_api.dto.LoginRequest;
import com.example.my_sports_store_api.dto.RegisterRequest;
import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.model.Role;
import com.example.my_sports_store_api.model.User;
import com.example.my_sports_store_api.repository.UserRepository;
import com.example.my_sports_store_api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_whenEmailDoesNotExist_savesUserAndReturnsAuthResponse() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getName()).isEqualTo("New User");
        assertThat(savedUser.getRoles()).containsExactly(Role.ROLE_USER);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.name()).isEqualTo("New User");
        assertThat(response.roles()).containsExactly("ROLE_USER");
    }

    @Test
    void register_whenEmailAlreadyExists_throwsBadRequestException() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "Existing User");
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .password("encoded-password")
                .name("Existing User")
                .roles(Set.of(Role.ROLE_USER))
                .build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(
                eq(new UsernamePasswordAuthenticationToken("user@example.com", "password123")));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.roles()).containsExactly("ROLE_USER");
    }

    @Test
    void login_whenUserNotFoundAfterAuthentication_throwsBadRequestException() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void register_neverPersistsRawPassword() {
        RegisterRequest request = new RegisterRequest("new@example.com", "plainPassword", "New User");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashed");
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("hashed");
    }
}
