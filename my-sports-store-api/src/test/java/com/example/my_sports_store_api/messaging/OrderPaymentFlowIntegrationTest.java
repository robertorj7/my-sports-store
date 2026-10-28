package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.TestcontainersConfiguration;
import com.example.my_sports_store_api.model.Cart;
import com.example.my_sports_store_api.model.CartItem;
import com.example.my_sports_store_api.model.Order;
import com.example.my_sports_store_api.model.OrderStatus;
import com.example.my_sports_store_api.model.Product;
import com.example.my_sports_store_api.model.User;
import com.example.my_sports_store_api.repository.CartRepository;
import com.example.my_sports_store_api.repository.OrderRepository;
import com.example.my_sports_store_api.repository.OutboxEventRepository;
import com.example.my_sports_store_api.repository.ProductRepository;
import com.example.my_sports_store_api.repository.UserRepository;
import com.example.my_sports_store_api.security.CurrentUserProvider;
import com.example.my_sports_store_api.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

// Test profile: payments up to 100 are approved and processed without delay
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrderPaymentFlowIntegrationTest {

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .password("secret")
                .name("Buyer")
                .build());
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    private Product product(BigDecimal price, int stock) {
        Product product = new Product();
        product.setName("Ball");
        product.setPrice(price);
        product.setStock(stock);
        return productRepository.save(product);
    }

    private Order checkout(Product product, int quantity) {
        Cart cart = new Cart(null, user.getId(), new ArrayList<>());
        cart.addItem(new CartItem(product.getId(), quantity));
        cartRepository.save(cart);
        return orderService.checkout();
    }

    private OrderStatus awaitFinalStatus(Order order) {
        await().atMost(Duration.ofSeconds(30)).until(() ->
                orderRepository.findById(order.getId()).orElseThrow().getStatus() != OrderStatus.PENDING);
        return orderRepository.findById(order.getId()).orElseThrow().getStatus();
    }

    @Test
    void checkout_whenPaymentIsApproved_orderBecomesPaid() {
        Product product = product(BigDecimal.valueOf(30), 5);

        Order order = checkout(product, 2);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(awaitFinalStatus(order)).isEqualTo(OrderStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
        await().atMost(Duration.ofSeconds(10)).until(() ->
                outboxEventRepository.findAll().stream()
                        .filter(event -> event.getMessageKey().equals(order.getId()))
                        .allMatch(event -> event.getPublishedAt() != null));
    }

    @Test
    void checkout_whenPaymentIsRejected_cancelsOrderAndRestoresStock() {
        Product product = product(BigDecimal.valueOf(80), 5);

        Order order = checkout(product, 2);

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
        assertThat(awaitFinalStatus(order)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }
}
