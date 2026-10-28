package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.event.OrderStatusChangedEvent;
import com.example.my_sports_store_api.model.OrderStatus;
import com.example.my_sports_store_api.model.User;
import com.example.my_sports_store_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private NotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationListener(userRepository, objectMapper);
    }

    private String payload() throws Exception {
        return objectMapper.writeValueAsString(new OrderStatusChangedEvent(
                "o1", "user-1", OrderStatus.PENDING, OrderStatus.PAID, Instant.now()));
    }

    @Test
    void onOrderStatusChanged_looksUpRecipient() throws Exception {
        when(userRepository.findById("user-1"))
                .thenReturn(Optional.of(User.builder().id("user-1").email("user@example.com").build()));

        listener.onOrderStatusChanged(payload());

        verify(userRepository).findById("user-1");
    }

    @Test
    void onOrderStatusChanged_whenUserNoLongerExists_skipsWithoutFailing() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());
        String payload = payload();

        assertThatCode(() -> listener.onOrderStatusChanged(payload)).doesNotThrowAnyException();
    }
}
