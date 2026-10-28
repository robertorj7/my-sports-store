package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.model.OutboxEvent;
import com.example.my_sports_store_api.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores events in the outbox as part of the caller's transaction. {@link OutboxRelay} sends them to Kafka.
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, String key, Object event) {
        outboxEventRepository.save(new OutboxEvent(topic, key, toJson(event)));
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize event " + event.getClass().getSimpleName(), ex);
        }
    }
}
