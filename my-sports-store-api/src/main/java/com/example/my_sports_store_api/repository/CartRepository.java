package com.example.my_sports_store_api.repository;

import com.example.my_sports_store_api.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {

    Optional<Cart> findByUserId(String userId);
}
