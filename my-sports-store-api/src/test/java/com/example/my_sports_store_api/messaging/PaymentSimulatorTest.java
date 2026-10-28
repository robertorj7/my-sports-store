package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.event.OrderCreatedEvent;
import com.example.my_sports_store_api.event.PaymentResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSimulatorTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private PaymentSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new PaymentSimulator(kafkaTemplate, objectMapper, BigDecimal.valueOf(100), 0);
    }

    private PaymentResultEvent process(BigDecimal total) throws Exception {
        when(kafkaTemplate.send(eq(Topics.PAYMENT_RESULT), eq("o1"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        String payload = objectMapper.writeValueAsString(new OrderCreatedEvent("o1", "user-1", total, Instant.now()));

        simulator.onOrderCreated(payload);

        ArgumentCaptor<String> result = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(Topics.PAYMENT_RESULT), eq("o1"), result.capture());
        return objectMapper.readValue(result.getValue(), PaymentResultEvent.class);
    }

    @Test
    void onOrderCreated_whenTotalIsWithinLimit_approvesPayment() throws Exception {
        PaymentResultEvent result = process(BigDecimal.valueOf(100));

        assertThat(result.orderId()).isEqualTo("o1");
        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isNull();
    }

    @Test
    void onOrderCreated_whenTotalExceedsLimit_rejectsPayment() throws Exception {
        PaymentResultEvent result = process(BigDecimal.valueOf(100.01));

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).contains("approval limit");
    }

    @Test
    void onOrderCreated_whenPayloadIsMalformed_throwsWithoutPublishing() {
        assertThatThrownBy(() -> simulator.onOrderCreated("not json"))
                .isInstanceOf(JsonProcessingException.class);

        verifyNoInteractions(kafkaTemplate);
    }
}
