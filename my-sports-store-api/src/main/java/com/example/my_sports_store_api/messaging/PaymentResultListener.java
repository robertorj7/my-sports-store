package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.event.PaymentResultEvent;
import com.example.my_sports_store_api.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentResultListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.PAYMENT_RESULT, groupId = "order-service")
    public void onPaymentResult(String payload) throws JsonProcessingException {
        orderService.applyPaymentResult(objectMapper.readValue(payload, PaymentResultEvent.class));
    }
}
