package com.example.my_sports_store_api.repository;

import com.example.my_sports_store_api.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
}
