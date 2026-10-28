package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.model.OutboxEvent;
import com.example.my_sports_store_api.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException ex) {
                // Stop here so later events are not sent ahead of this one; it is retried on the next run
                log.warn("Could not publish outbox event {} to {}: {}", event.getId(), event.getTopic(), ex.getMessage());
                return;
            }
            event.setPublishedAt(Instant.now());
        }
    }
}
