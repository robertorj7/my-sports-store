package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.event.OrderStatusChangedEvent;
import com.example.my_sports_store_api.model.User;
import com.example.my_sports_store_api.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Notifies customers about order status changes. Logs the message in place of sending an e-mail.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.ORDER_STATUS_CHANGED, groupId = "notification-service")
    public void onOrderStatusChanged(String payload) throws JsonProcessingException {
        OrderStatusChangedEvent event = objectMapper.readValue(payload, OrderStatusChangedEvent.class);
        String recipient = userRepository.findById(event.userId()).map(User::getEmail).orElse(null);
        if (recipient == null) {
            log.warn("Skipping notification for order {}: user {} not found", event.orderId(), event.userId());
            return;
        }
        log.info("Notifying {}: order {} changed from {} to {}",
                recipient, event.orderId(), event.previousStatus(), event.newStatus());
    }
}
