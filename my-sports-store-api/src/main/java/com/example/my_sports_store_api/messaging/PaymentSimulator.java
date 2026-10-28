package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.event.OrderCreatedEvent;
import com.example.my_sports_store_api.event.PaymentResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stands in for an external payment gateway: approves orders up to a configured amount and rejects the rest.
 */
@Slf4j
@Component
public class PaymentSimulator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final BigDecimal approvalLimit;
    private final long processingDelayMs;

    public PaymentSimulator(KafkaTemplate<String, String> kafkaTemplate,
                            ObjectMapper objectMapper,
                            @Value("${app.payment.approval-limit}") BigDecimal approvalLimit,
                            @Value("${app.payment.processing-delay-ms}") long processingDelayMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.approvalLimit = approvalLimit;
        this.processingDelayMs = processingDelayMs;
    }

    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = "payment-service")
    public void onOrderCreated(String payload) throws JsonProcessingException, InterruptedException {
        OrderCreatedEvent order = objectMapper.readValue(payload, OrderCreatedEvent.class);
        Thread.sleep(processingDelayMs);

        boolean approved = order.total().compareTo(approvalLimit) <= 0;
        PaymentResultEvent result = new PaymentResultEvent(
                order.orderId(), approved, approved ? null : "Amount exceeds the approval limit");
        log.info("Payment for order {} {}", order.orderId(), approved ? "approved" : "rejected");

        kafkaTemplate.send(Topics.PAYMENT_RESULT, order.orderId(), objectMapper.writeValueAsString(result)).join();
    }
}
