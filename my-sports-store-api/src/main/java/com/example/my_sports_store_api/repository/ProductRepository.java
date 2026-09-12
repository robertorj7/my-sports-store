package com.example.my_sports_store_api.repository;

import com.example.my_sports_store_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategoryIgnoreCaseAndNameContainingIgnoreCase(String category, String name);
}
