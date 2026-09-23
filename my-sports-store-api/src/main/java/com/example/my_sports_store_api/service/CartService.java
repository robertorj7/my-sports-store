package com.example.my_sports_store_api.service;

import com.example.my_sports_store_api.dto.CartItemRequest;
import com.example.my_sports_store_api.dto.CartResponse;
import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.model.Cart;
import com.example.my_sports_store_api.model.CartItem;
import com.example.my_sports_store_api.model.Product;
import com.example.my_sports_store_api.repository.CartRepository;
import com.example.my_sports_store_api.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;
    private final CurrentUserProvider currentUserProvider;

    public CartResponse getCart() {
        return toResponse(getOrCreateCart());
    }

    public CartResponse addOrUpdateItem(CartItemRequest request) {
        Product product = productService.findById(request.productId());
        if (product.getStock() < request.quantity()) {
            throw new BadRequestException("Not enough stock for " + product.getName());
        }

        Cart cart = getOrCreateCart();
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.productId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(request.quantity());
        } else {
            cart.addItem(new CartItem(request.productId(), request.quantity()));
        }

        cartRepository.save(cart);
        return toResponse(cart);
    }

    public CartResponse removeItem(String productId) {
        Cart cart = getOrCreateCart();
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cartRepository.save(cart);
        return toResponse(cart);
    }

    public CartResponse clearCart() {
        Cart cart = getOrCreateCart();
        cart.getItems().clear();
        cartRepository.save(cart);
        return toResponse(cart);
    }

    Cart getOrCreateCart() {
        String userId = currentUserProvider.getCurrentUser().getId();
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(null, userId, new ArrayList<>())));
    }

    private CartResponse toResponse(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return new CartResponse(List.of(), BigDecimal.ZERO);
        }

        Map<String, Product> products = productService.findAllByIds(
                cart.getItems().stream().map(CartItem::getProductId).toList());

        List<CartResponse.CartLineItem> lineItems = cart.getItems().stream()
                .filter(item -> products.containsKey(item.getProductId()))
                .map(item -> {
                    Product product = products.get(item.getProductId());
                    BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartResponse.CartLineItem(
                            product.getId(), product.getName(), product.getImage(),
                            product.getPrice(), item.getQuantity(), lineTotal);
                })
                .collect(Collectors.toList());

        BigDecimal total = lineItems.stream()
                .map(CartResponse.CartLineItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(lineItems, total);
    }
}
