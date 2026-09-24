package com.example.my_sports_store_api.controller;

import com.example.my_sports_store_api.dto.ProductRequest;
import com.example.my_sports_store_api.exception.GlobalExceptionHandler;
import com.example.my_sports_store_api.exception.ResourceNotFoundException;
import com.example.my_sports_store_api.model.Product;
import com.example.my_sports_store_api.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Product product(String id, String name) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(BigDecimal.TEN);
        product.setStock(5);
        return product;
    }

    @Test
    void findProducts_withCategoryAndSearch_delegatesParamsToService() throws Exception {
        when(productService.findProducts("shoes", "run")).thenReturn(List.of(product("1", "Running Shoes")));

        mockMvc.perform(get("/api/products").param("category", "shoes").param("search", "run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Running Shoes"));

        verify(productService).findProducts("shoes", "run");
    }

    @Test
    void findCategories_returnsListFromService() throws Exception {
        when(productService.findCategories()).thenReturn(List.of("shoes", "apparel"));

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("shoes"));
    }

    @Test
    void findById_whenFound_returnsProduct() throws Exception {
        when(productService.findById("1")).thenReturn(product("1", "Ball"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ball"));
    }

    @Test
    void findById_whenNotFound_returns404() throws Exception {
        when(productService.findById("missing")).thenThrow(new ResourceNotFoundException("Product not found: missing"));

        mockMvc.perform(get("/api/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: missing"));
    }

    @Test
    void create_withValidBody_returns201() throws Exception {
        ProductRequest request = new ProductRequest("Ball", "desc", BigDecimal.valueOf(19.99),
                "img.png", "red", "sports", 5);
        when(productService.create(any(ProductRequest.class))).thenReturn(product("1", "Ball"));

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ball"));
    }

    @Test
    void create_withInvalidBody_returns400() throws Exception {
        ProductRequest invalidRequest = new ProductRequest("", "desc", BigDecimal.valueOf(-1),
                "img.png", "red", "", -1);

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_withValidBody_returnsUpdatedProduct() throws Exception {
        ProductRequest request = new ProductRequest("Ball v2", "desc", BigDecimal.valueOf(29.99),
                "img.png", "blue", "sports", 3);
        when(productService.update(eq("1"), any(ProductRequest.class))).thenReturn(product("1", "Ball v2"));

        mockMvc.perform(put("/api/products/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ball v2"));
    }

    @Test
    void delete_removesProductAndReturns204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).delete("1");
    }

    @Test
    void delete_whenProductDoesNotExist_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Product not found: missing"))
                .when(productService).delete("missing");

        mockMvc.perform(delete("/api/products/missing"))
                .andExpect(status().isNotFound());
    }
}
