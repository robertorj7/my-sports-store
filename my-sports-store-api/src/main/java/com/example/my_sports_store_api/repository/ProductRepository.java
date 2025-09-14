package com.example.my_sports_store_api.repository;

import com.example.my_sports_store_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}
