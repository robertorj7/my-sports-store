package com.example.my_sports_store_api.service;

import com.example.my_sports_store_api.dto.ProductRequest;
import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.ResourceNotFoundException;
import com.example.my_sports_store_api.model.Product;
import com.example.my_sports_store_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findProducts(String category, String search) {
        boolean hasCategory = StringUtils.hasText(category);
        boolean hasSearch = StringUtils.hasText(search);

        if (hasCategory && hasSearch) {
            return productRepository.findByCategoryIgnoreCaseAndNameContainingIgnoreCase(category, search);
        }
        if (hasCategory) {
            return productRepository.findByCategoryIgnoreCase(category);
        }
        if (hasSearch) {
            return productRepository.findByNameContainingIgnoreCase(search);
        }
        return productRepository.findAll();
    }

    public List<Product> findPromotions() {
        return productRepository.findByPromotionalPriceIsNotNull().stream()
                .filter(Product::isOnPromotion)
                .sorted(Comparator.comparing(Product::getDiscountRatio).reversed())
                .collect(Collectors.toList());
    }

    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public Map<String, Product> findAllByIds(List<String> ids) {
        return productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    public List<String> findCategories() {
        return productRepository.findAll().stream()
                .map(Product::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public Product create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return productRepository.save(product);
    }

    public Product update(String id, ProductRequest request) {
        Product product = findById(id);
        applyRequest(product, request);
        return productRepository.save(product);
    }

    public void delete(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    private void applyRequest(Product product, ProductRequest request) {
        if (request.promotionalPrice() != null && request.promotionalPrice().compareTo(request.price()) >= 0) {
            throw new BadRequestException("Promotional price must be lower than the regular price");
        }
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImage(request.image());
        product.setColor(request.color());
        product.setCategory(request.category());
        product.setStock(request.stock());
        product.setPromotionalPrice(request.promotionalPrice());
    }
}
