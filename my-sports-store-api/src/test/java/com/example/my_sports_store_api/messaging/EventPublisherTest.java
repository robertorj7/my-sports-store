package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.event.PaymentResultEvent;
import com.example.my_sports_store_api.model.OutboxEvent;
import com.example.my_sports_store_api.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void publish_storesSerializedEventAsPendingOutboxEntry() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        EventPublisher publisher = new EventPublisher(outboxEventRepository, objectMapper);
        PaymentResultEvent event = new PaymentResultEvent("o1", true, null);

        publisher.publish(Topics.PAYMENT_RESULT, "o1", event);

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(saved.capture());
        assertThat(saved.getValue().getTopic()).isEqualTo(Topics.PAYMENT_RESULT);
        assertThat(saved.getValue().getMessageKey()).isEqualTo("o1");
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
        assertThat(saved.getValue().getPublishedAt()).isNull();
        assertThat(objectMapper.readValue(saved.getValue().getPayload(), PaymentResultEvent.class)).isEqualTo(event);
    }
}
