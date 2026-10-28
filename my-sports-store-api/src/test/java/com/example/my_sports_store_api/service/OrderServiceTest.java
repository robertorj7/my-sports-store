package com.example.my_sports_store_api.service;

import com.example.my_sports_store_api.event.OrderCreatedEvent;
import com.example.my_sports_store_api.event.OrderStatusChangedEvent;
import com.example.my_sports_store_api.event.PaymentResultEvent;
import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.ResourceNotFoundException;
import com.example.my_sports_store_api.messaging.EventPublisher;
import com.example.my_sports_store_api.messaging.Topics;
import com.example.my_sports_store_api.model.Cart;
import com.example.my_sports_store_api.model.CartItem;
import com.example.my_sports_store_api.model.Order;
import com.example.my_sports_store_api.model.OrderItem;
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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private EventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(cartService, cartRepository, productRepository, orderRepository, currentUserProvider, eventPublisher);
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
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void checkout_whenProductNoLongerExists_throwsResourceNotFoundException() {
        Cart cart = new Cart(null, "user-1", new ArrayList<>(List.of(new CartItem("missing", 1))));
        when(cartService.getOrCreateCart()).thenReturn(cart);
        when(productRepository.findAllByIdForUpdate(List.of("missing"))).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void checkout_whenInsufficientStock_throwsBadRequestException() {
        Cart cart = new Cart(null, "user-1", new ArrayList<>(List.of(new CartItem("p1", 5))));
        when(cartService.getOrCreateCart()).thenReturn(cart);
        Product product = product("p1", "Ball", BigDecimal.TEN, 2);
        when(productRepository.findAllByIdForUpdate(List.of("p1"))).thenReturn(List.of(product));

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
        when(productRepository.findAllByIdForUpdate(List.of("p1"))).thenReturn(List.of(product));
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
    void checkout_whenSuccessful_publishesOrderCreatedEvent() {
        Cart cart = new Cart(null, "user-1", new ArrayList<>(List.of(new CartItem("p1", 2))));
        when(cartService.getOrCreateCart()).thenReturn(cart);
        when(productRepository.findAllByIdForUpdate(List.of("p1")))
                .thenReturn(List.of(product("p1", "Ball", BigDecimal.valueOf(10), 5)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId("o1");
            return saved;
        });

        orderService.checkout();

        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publish(eq(Topics.ORDER_CREATED), eq("o1"), event.capture());
        assertThat(event.getValue().orderId()).isEqualTo("o1");
        assertThat(event.getValue().userId()).isEqualTo("user-1");
        assertThat(event.getValue().total()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void checkout_whenProductIsOnPromotion_chargesPromotionalPrice() {
        Cart cart = new Cart(null, "user-1", new ArrayList<>(List.of(new CartItem("p1", 2))));
        when(cartService.getOrCreateCart()).thenReturn(cart);
        Product product = product("p1", "Ball", BigDecimal.valueOf(10), 5);
        product.setPromotionalPrice(BigDecimal.valueOf(7));
        when(productRepository.findAllByIdForUpdate(List.of("p1"))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.checkout();

        assertThat(order.getItems().get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(7));
        assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(14));
    }

    @Test
    void checkout_whenPromotionalPriceIsNotLower_chargesRegularPrice() {
        Cart cart = new Cart(null, "user-1", new ArrayList<>(List.of(new CartItem("p1", 1))));
        when(cartService.getOrCreateCart()).thenReturn(cart);
        Product product = product("p1", "Ball", BigDecimal.valueOf(10), 5);
        product.setPromotionalPrice(BigDecimal.valueOf(10));
        when(productRepository.findAllByIdForUpdate(List.of("p1"))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.checkout();

        assertThat(order.getItems().get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(10));
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

    private Order pendingOrder(String productId, int quantity) {
        OrderItem item = new OrderItem(productId, "Ball", BigDecimal.TEN, quantity);
        Order order = new Order("o1", "user-1", new ArrayList<>(List.of(item)), BigDecimal.TEN, OrderStatus.PENDING, Instant.now());
        item.setOrder(order);
        return order;
    }

    @Test
    void applyPaymentResult_whenApproved_marksOrderPaidAndPublishesStatusChange() {
        Order order = pendingOrder("p1", 2);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent("o1", true, null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
        verify(productRepository, never()).saveAll(anyCollection());
        ArgumentCaptor<OrderStatusChangedEvent> event = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(eventPublisher).publish(eq(Topics.ORDER_STATUS_CHANGED), eq("o1"), event.capture());
        assertThat(event.getValue().previousStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(event.getValue().newStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(event.getValue().userId()).isEqualTo("user-1");
    }

    @Test
    void applyPaymentResult_whenRejected_cancelsOrderAndRestoresStock() {
        Order order = pendingOrder("p1", 2);
        Product product = product("p1", "Ball", BigDecimal.TEN, 3);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(productRepository.findAllByIdForUpdate(List.of("p1"))).thenReturn(List.of(product));

        orderService.applyPaymentResult(new PaymentResultEvent("o1", false, "Amount exceeds the approval limit"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(5);
        verify(productRepository).saveAll(anyCollection());
        verify(eventPublisher).publish(eq(Topics.ORDER_STATUS_CHANGED), eq("o1"), any(OrderStatusChangedEvent.class));
    }

    @Test
    void applyPaymentResult_whenProductWasDeleted_cancelsOrderWithoutFailing() {
        Order order = pendingOrder("gone", 2);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(productRepository.findAllByIdForUpdate(List.of("gone"))).thenReturn(List.of());

        orderService.applyPaymentResult(new PaymentResultEvent("o1", false, "rejected"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void applyPaymentResult_whenOrderIsNoLongerPending_ignoresDuplicate() {
        Order order = pendingOrder("p1", 2);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent("o1", false, "rejected"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void applyPaymentResult_whenOrderNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.applyPaymentResult(new PaymentResultEvent("missing", true, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_whenTransitionIsAllowed_changesStatusAndPublishesEvent() {
        Order order = pendingOrder("p1", 1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        Order result = orderService.updateStatus("o1", OrderStatus.SHIPPED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(eventPublisher).publish(eq(Topics.ORDER_STATUS_CHANGED), eq("o1"), any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateStatus_whenTransitionIsNotAllowed_throwsBadRequestException() {
        Order order = pendingOrder("p1", 1);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus("o1", OrderStatus.DELIVERED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING to DELIVERED");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void findAll_returnsEveryOrderNewestFirst() {
        Order order = pendingOrder("p1", 1);
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        assertThat(orderService.findAll()).containsExactly(order);
    }
}
