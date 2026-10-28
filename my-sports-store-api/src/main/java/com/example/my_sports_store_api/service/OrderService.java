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
import com.example.my_sports_store_api.repository.CartRepository;
import com.example.my_sports_store_api.repository.OrderRepository;
import com.example.my_sports_store_api.repository.ProductRepository;
import com.example.my_sports_store_api.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserProvider currentUserProvider;
    private final EventPublisher eventPublisher;

    public Order checkout() {
        String userId = currentUserProvider.getCurrentUser().getId();
        Cart cart = cartService.getOrCreateCart();

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout an empty cart");
        }

        // Row locks keep concurrent checkouts from selling the same units twice
        Map<String, Product> products = productRepository
                .findAllByIdForUpdate(cart.getItems().stream().map(CartItem::getProductId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));

        List<OrderItem> orderItems = cart.getItems().stream().map(item -> {
            Product product = products.get(item.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + item.getProductId());
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BadRequestException("Not enough stock for " + product.getName());
            }
            return new OrderItem(product.getId(), product.getName(), product.getEffectivePrice(), item.getQuantity());
        }).toList();

        orderItems.forEach(item -> {
            Product product = products.get(item.getProductId());
            product.setStock(product.getStock() - item.getQuantity());
        });
        productRepository.saveAll(products.values());

        BigDecimal total = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order(null, userId, orderItems, total, OrderStatus.PENDING, Instant.now());
        orderItems.forEach(item -> item.setOrder(order));
        orderRepository.save(order);
        eventPublisher.publish(Topics.ORDER_CREATED, order.getId(),
                new OrderCreatedEvent(order.getId(), userId, total, order.getCreatedAt()));

        cart.getItems().clear();
        cartRepository.save(cart);

        return order;
    }

    public void applyPaymentResult(PaymentResultEvent result) {
        Order order = findOrder(result.orderId());
        // Kafka delivers at least once, so the same result may arrive again after it was applied
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Ignoring payment result for order {} already in status {}", order.getId(), order.getStatus());
            return;
        }
        if (!result.approved()) {
            log.info("Cancelling order {}: {}", order.getId(), result.reason());
        }
        changeStatus(order, result.approved() ? OrderStatus.PAID : OrderStatus.CANCELLED);
    }

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order updateStatus(String id, OrderStatus status) {
        return changeStatus(findOrder(id), status);
    }

    public List<Order> findMyOrders() {
        String userId = currentUserProvider.getCurrentUser().getId();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order findMyOrder(String id) {
        String userId = currentUserProvider.getCurrentUser().getId();
        Order order = findOrder(id);
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        return order;
    }

    private Order findOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Order changeStatus(Order order, OrderStatus newStatus) {
        OrderStatus previousStatus = order.getStatus();
        if (!previousStatus.canTransitionTo(newStatus)) {
            throw new BadRequestException("Cannot change order status from " + previousStatus + " to " + newStatus);
        }
        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
        eventPublisher.publish(Topics.ORDER_STATUS_CHANGED, order.getId(), new OrderStatusChangedEvent(
                order.getId(), order.getUserId(), previousStatus, newStatus, Instant.now()));
        return order;
    }

    private void restoreStock(Order order) {
        Map<String, Product> products = productRepository
                .findAllByIdForUpdate(order.getItems().stream().map(OrderItem::getProductId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
        // Products deleted since the purchase have no stock to give back
        order.getItems().forEach(item -> {
            Product product = products.get(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
            }
        });
        productRepository.saveAll(products.values());
    }
}
