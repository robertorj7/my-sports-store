package com.example.my_sports_store_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "products", indexes = @Index(name = "idx_products_category", columnList = "category"))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;
    private String description;
    @Column(nullable = false)
    private BigDecimal price;
    private String image;
    private String color;
    private String category;
    private int stock;
    private BigDecimal promotionalPrice;

    public BigDecimal getEffectivePrice() {
        return isOnPromotion() ? promotionalPrice : price;
    }

    public boolean isOnPromotion() {
        return promotionalPrice != null && price != null && promotionalPrice.compareTo(price) < 0;
    }

    @JsonIgnore
    public BigDecimal getDiscountRatio() {
        return isOnPromotion()
                ? price.subtract(promotionalPrice).divide(price, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
}
