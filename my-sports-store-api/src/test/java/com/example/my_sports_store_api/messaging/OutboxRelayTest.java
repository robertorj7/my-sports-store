package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.model.OutboxEvent;
import com.example.my_sports_store_api.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outboxEventRepository, kafkaTemplate);
    }

    @Test
    void publishPending_sendsEventsInOrderAndMarksThemPublished() {
        OutboxEvent first = new OutboxEvent(Topics.ORDER_CREATED, "o1", "{\"n\":1}");
        OutboxEvent second = new OutboxEvent(Topics.ORDER_STATUS_CHANGED, "o1", "{\"n\":2}");
        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(Topics.ORDER_CREATED, "o1", "{\"n\":1}")).thenReturn(sent());
        when(kafkaTemplate.send(Topics.ORDER_STATUS_CHANGED, "o1", "{\"n\":2}")).thenReturn(sent());

        relay.publishPending();

        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPending_whenSendFails_stopsAndLeavesRemainingEventsPending() {
        OutboxEvent first = new OutboxEvent(Topics.ORDER_CREATED, "o1", "a");
        OutboxEvent second = new OutboxEvent(Topics.ORDER_CREATED, "o2", "b");
        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(Topics.ORDER_CREATED, "o1", "a"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        relay.publishPending();

        assertThat(first.getPublishedAt()).isNull();
        assertThat(second.getPublishedAt()).isNull();
        verify(kafkaTemplate, never()).send(Topics.ORDER_CREATED, "o2", "b");
    }

    private static CompletableFuture<SendResult<String, String>> sent() {
        return CompletableFuture.completedFuture(null);
    }
}
