package com.example.my_sports_store_api.service;

import com.example.my_sports_store_api.dto.ProductRequest;
import com.example.my_sports_store_api.exception.ResourceNotFoundException;
import com.example.my_sports_store_api.model.Product;
import com.example.my_sports_store_api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    private Product product(String id, String name, String category) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCategory(category);
        product.setPrice(BigDecimal.TEN);
        product.setStock(10);
        return product;
    }

    @Test
    void findProducts_withCategoryAndSearch_usesCombinedQuery() {
        when(productRepository.findByCategoryIgnoreCaseAndNameContainingIgnoreCase("shoes", "run"))
                .thenReturn(List.of(product("1", "Running Shoes", "shoes")));

        List<Product> result = productService.findProducts("shoes", "run");

        assertThat(result).hasSize(1);
        verify(productRepository).findByCategoryIgnoreCaseAndNameContainingIgnoreCase("shoes", "run");
        verify(productRepository, never()).findAll();
    }

    @Test
    void findProducts_withCategoryOnly_filtersByCategory() {
        when(productRepository.findByCategoryIgnoreCase("shoes"))
                .thenReturn(List.of(product("1", "Running Shoes", "shoes")));

        List<Product> result = productService.findProducts("shoes", null);

        assertThat(result).hasSize(1);
        verify(productRepository).findByCategoryIgnoreCase("shoes");
    }

    @Test
    void findProducts_withSearchOnly_filtersByName() {
        when(productRepository.findByNameContainingIgnoreCase("run"))
                .thenReturn(List.of(product("1", "Running Shoes", "shoes")));

        List<Product> result = productService.findProducts("", "run");

        assertThat(result).hasSize(1);
        verify(productRepository).findByNameContainingIgnoreCase("run");
    }

    @Test
    void findProducts_withNoFilters_returnsAll() {
        when(productRepository.findAll()).thenReturn(List.of(product("1", "Ball", "sports")));

        List<Product> result = productService.findProducts(null, "  ");

        assertThat(result).hasSize(1);
        verify(productRepository).findAll();
    }

    @Test
    void findById_whenFound_returnsProduct() {
        Product product = product("1", "Ball", "sports");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));

        Product result = productService.findById("1");

        assertThat(result).isEqualTo(product);
    }

    @Test
    void findById_whenNotFound_throwsResourceNotFoundException() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void findAllByIds_returnsMapKeyedById() {
        Product p1 = product("1", "Ball", "sports");
        Product p2 = product("2", "Bat", "sports");
        when(productRepository.findAllById(List.of("1", "2"))).thenReturn(List.of(p1, p2));

        var result = productService.findAllByIds(List.of("1", "2"));

        assertThat(result).containsEntry("1", p1).containsEntry("2", p2);
    }

    @Test
    void findCategories_returnsDistinctSortedNonBlankCategories() {
        when(productRepository.findAll()).thenReturn(List.of(
                product("1", "Ball", "sports"),
                product("2", "Bat", "sports"),
                product("3", "Shirt", "apparel"),
                product("4", "NoCategory", "")
        ));

        List<String> categories = productService.findCategories();

        assertThat(categories).containsExactly("apparel", "sports");
    }

    @Test
    void create_savesNewProductWithRequestValues() {
        ProductRequest request = new ProductRequest("Ball", "desc", BigDecimal.valueOf(19.99),
                "img.png", "red", "sports", 5);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.create(request);

        assertThat(result.getName()).isEqualTo("Ball");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(19.99));
        assertThat(result.getCategory()).isEqualTo("sports");
        assertThat(result.getStock()).isEqualTo(5);
    }

    @Test
    void update_whenProductExists_updatesAndSaves() {
        Product existing = product("1", "Old Name", "sports");
        ProductRequest request = new ProductRequest("New Name", "desc", BigDecimal.valueOf(29.99),
                "img.png", "blue", "apparel", 3);
        when(productRepository.findById("1")).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        Product result = productService.update("1", request);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getCategory()).isEqualTo("apparel");
        assertThat(result.getStock()).isEqualTo(3);
        verify(productRepository).save(existing);
    }

    @Test
    void update_whenProductDoesNotExist_throwsResourceNotFoundException() {
        ProductRequest request = new ProductRequest("New Name", "desc", BigDecimal.TEN,
                "img.png", "blue", "apparel", 3);
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update("missing", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void delete_whenProductExists_deletesById() {
        when(productRepository.existsById("1")).thenReturn(true);

        productService.delete("1");

        verify(productRepository, times(1)).deleteById("1");
    }

    @Test
    void delete_whenProductDoesNotExist_throwsResourceNotFoundException() {
        when(productRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> productService.delete("missing"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(anyString());
    }
}
