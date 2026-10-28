package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.event.PaymentResultEvent;
import com.example.my_sports_store_api.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentResultListenerTest {

    @Mock
    private OrderService orderService;

    @Test
    void onPaymentResult_appliesResultToOrder() throws Exception {
        PaymentResultListener listener = new PaymentResultListener(orderService, new ObjectMapper());

        listener.onPaymentResult("{\"orderId\":\"o1\",\"approved\":false,\"reason\":\"limit\"}");

        verify(orderService).applyPaymentResult(new PaymentResultEvent("o1", false, "limit"));
    }
}
