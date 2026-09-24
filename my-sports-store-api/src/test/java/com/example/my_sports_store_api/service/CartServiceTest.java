package com.example.my_sports_store_api.service;

import com.example.my_sports_store_api.dto.CartItemRequest;
import com.example.my_sports_store_api.dto.CartResponse;
import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.model.Cart;
import com.example.my_sports_store_api.model.CartItem;
import com.example.my_sports_store_api.model.Product;
import com.example.my_sports_store_api.model.User;
import com.example.my_sports_store_api.repository.CartRepository;
import com.example.my_sports_store_api.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductService productService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productService, currentUserProvider);
        User user = User.builder().id("user-1").email("user@example.com").build();
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    private Product product(String id, String name, BigDecimal price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setImage("img.png");
        return product;
    }

    @Test
    void getCart_whenNoCartExists_createsEmptyCart() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        Cart newCart = new Cart(null, "user-1", new java.util.ArrayList<>());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        CartResponse response = cartService.getCart();

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCart_whenCartHasItems_returnsLineItemsAndTotal() {
        Cart cart = new Cart(null, "user-1", new java.util.ArrayList<>(List.of(new CartItem("p1", 2))));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        Product product = product("p1", "Ball", BigDecimal.valueOf(10), 5);
        when(productService.findAllByIds(List.of("p1"))).thenReturn(Map.of("p1", product));

        CartResponse response = cartService.getCart();

        assertThat(response.items()).hasSize(1);
        CartResponse.CartLineItem lineItem = response.items().get(0);
        assertThat(lineItem.productId()).isEqualTo("p1");
        assertThat(lineItem.quantity()).isEqualTo(2);
        assertThat(lineItem.lineTotal()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void getCart_whenProductNoLongerExists_excludesItFromResponse() {
        Cart cart = new Cart(null, "user-1", new java.util.ArrayList<>(List.of(new CartItem("deleted", 1))));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productService.findAllByIds(List.of("deleted"))).thenReturn(Map.of());

        CartResponse response = cartService.getCart();

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void addOrUpdateItem_whenProductHasInsufficientStock_throwsBadRequestException() {
        CartItemRequest request = new CartItemRequest("p1", 10);
        Product product = product("p1", "Ball", BigDecimal.TEN, 3);
        when(productService.findById("p1")).thenReturn(product);

        assertThatThrownBy(() -> cartService.addOrUpdateItem(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Ball");

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addOrUpdateItem_whenNewProduct_addsItemToCart() {
        CartItemRequest request = new CartItemRequest("p1", 2);
        Product product = product("p1", "Ball", BigDecimal.TEN, 5);
        when(productService.findById("p1")).thenReturn(product);
        Cart cart = new Cart(null, "user-1", new java.util.ArrayList<>());
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productService.findAllByIds(anyList())).thenReturn(Map.of("p1", product));

        CartResponse response = cartService.addOrUpdateItem(request);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProductId()).isEqualTo("p1");
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.items()).hasSize(1);
        verify(cartRepository).save(cart);
    }

    @Test
    void addOrUpdateItem_whenProductAlreadyInCart_updatesQuantityInstead() {
        CartItemRequest request = new CartItemRequest("p1", 5);
        Product product = product("p1", "Ball", BigDecimal.TEN, 10);
        when(productService.findById("p1")).thenReturn(product);
        Cart cart = new Cart(null, "user-1", new java.util.ArrayList<>(List.of(new CartItem("p1", 1))));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productService.findAllByIds(anyList())).thenReturn(Map.of("p1", product));

        cartService.addOrUpdateItem(request);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void removeItem_removesOnlyMatchingProduct() {
        Cart cart = new Cart(null, "user-1", new java.util.ArrayList<>(
                List.of(new CartItem("p1", 1), new CartItem("p2", 3))));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productService.findAllByIds(anyList())).thenReturn(Map.of());

        cartService.removeItem("p1");

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProductId()).isEqualTo("p2");
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_removesAllItems() {
        Cart cart = new Cart(null, "user-1", new java.util.ArrayList<>(
                List.of(new CartItem("p1", 1), new CartItem("p2", 3))));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart();

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(cartRepository).save(cart);
    }
}
