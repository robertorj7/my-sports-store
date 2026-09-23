package com.example.my_sports_store_api.service;

import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.ResourceNotFoundException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserProvider currentUserProvider;

    public Order checkout() {
        String userId = currentUserProvider.getCurrentUser().getId();
        Cart cart = cartService.getOrCreateCart();

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout an empty cart");
        }

        Map<String, Product> products = productRepository
                .findAllById(cart.getItems().stream().map(CartItem::getProductId).toList())
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
            return new OrderItem(product.getId(), product.getName(), product.getPrice(), item.getQuantity());
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

        cart.getItems().clear();
        cartRepository.save(cart);

        return order;
    }

    public List<Order> findMyOrders() {
        String userId = currentUserProvider.getCurrentUser().getId();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order findMyOrder(String id) {
        String userId = currentUserProvider.getCurrentUser().getId();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        return order;
    }
}
