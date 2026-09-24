package com.example.my_sports_store_api.service;

import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.ResourceNotFoundException;
import com.example.my_sports_store_api.model.Cart;
import com.example.my_sports_store_api.model.CartItem;
import com.example.my_sports_store_api.model.Order;
import com.example.my_sports_store_api.model.OrderStatus;
import com.example.my_sports_store_api.model.Product;
import com.example.my_sports_store_api.model.User;
import com.example.my_sports_store_api.repository.CartRepository;
import com.example.my_sports_store_api.repository.OrderRepository;
import com.example.my_sports_store_api.repository.ProductRepository;
import com.example.my_sports_store_api.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartService cartService;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(cartService, cartRepository, productRepository, orderRepository, currentUserProvider);
        User user = User.builder().id("user-1").email("user@example.com").build();
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    private Product product(String id, String name, BigDecimal price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        return product;
    }

    @Test
    void checkout_whenCartIsEmpty_throwsBadRequestException() {
        when(cartService.getOrCreateCart()).thenReturn(new Cart(null, "user-1", new ArrayList<>()));

        assertThatThrownBy(() -> orderService.checkout())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty cart");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_whenProductNoLongerExists_throwsResourceNotFoundException() {
        Cart cart = new Cart(null, "user-1", new ArrayList<>(List.of(new CartItem("missing", 1))));
        when(cartService.getOrCreateCart()).thenReturn(cart);
        when(productRepository.findAllById(List.of("missing"))).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void checkout_whenInsufficientStock_throwsBadRequestException() {
        Cart cart = new Cart(null, "user-1", new ArrayList<>(List.of(new CartItem("p1", 5))));
        when(cartService.getOrCreateCart()).thenReturn(cart);
        Product product = product("p1", "Ball", BigDecimal.TEN, 2);
        when(productRepository.findAllById(List.of("p1"))).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.checkout())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Ball");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_whenSuccessful_decrementsStockCreatesOrderAndClearsCart() {
        Cart cart = new Cart(null, "user-1", new ArrayList<>(List.of(new CartItem("p1", 2))));
        when(cartService.getOrCreateCart()).thenReturn(cart);
        Product product = product("p1", "Ball", BigDecimal.valueOf(10), 5);
        when(productRepository.findAllById(List.of("p1"))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.checkout();

        assertThat(product.getStock()).isEqualTo(3);
        assertThat(order.getUserId()).isEqualTo("user-1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getProductId()).isEqualTo("p1");
        assertThat(cart.getItems()).isEmpty();
        verify(productRepository).saveAll(anyCollection());
        verify(cartRepository).save(cart);
    }

    @Test
    void findMyOrders_returnsOrdersForCurrentUserSortedByRepository() {
        Order order = new Order("o1", "user-1", List.of(), BigDecimal.TEN, OrderStatus.PENDING, Instant.now());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(order));

        List<Order> orders = orderService.findMyOrders();

        assertThat(orders).containsExactly(order);
    }

    @Test
    void findMyOrder_whenFoundAndOwnedByUser_returnsOrder() {
        Order order = new Order("o1", "user-1", List.of(), BigDecimal.TEN, OrderStatus.PENDING, Instant.now());
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        Order result = orderService.findMyOrder("o1");

        assertThat(result).isEqualTo(order);
    }

    @Test
    void findMyOrder_whenNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findMyOrder("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findMyOrder_whenOwnedByAnotherUser_throwsResourceNotFoundException() {
        Order order = new Order("o1", "other-user", List.of(), BigDecimal.TEN, OrderStatus.PENDING, Instant.now());
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.findMyOrder("o1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
