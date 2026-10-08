package com.example.my_sports_store_api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    private Product product(String price, String promotionalPrice) {
        Product product = new Product();
        product.setPrice(price == null ? null : new BigDecimal(price));
        product.setPromotionalPrice(promotionalPrice == null ? null : new BigDecimal(promotionalPrice));
        return product;
    }

    @Test
    void withoutPromotionalPrice_isNotOnPromotionAndChargesRegularPrice() {
        Product product = product("100.00", null);

        assertThat(product.isOnPromotion()).isFalse();
        assertThat(product.getEffectivePrice()).isEqualByComparingTo("100.00");
        assertThat(product.getDiscountRatio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void withLowerPromotionalPrice_isOnPromotionAndChargesPromotionalPrice() {
        Product product = product("100.00", "75.00");

        assertThat(product.isOnPromotion()).isTrue();
        assertThat(product.getEffectivePrice()).isEqualByComparingTo("75.00");
        assertThat(product.getDiscountRatio()).isEqualByComparingTo("0.25");
    }

    @Test
    void withPromotionalPriceEqualToPrice_isNotOnPromotion() {
        Product product = product("100.00", "100.00");

        assertThat(product.isOnPromotion()).isFalse();
        assertThat(product.getEffectivePrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void withPromotionalPriceHigherThanPrice_ignoresPromotionalPrice() {
        Product product = product("100.00", "120.00");

        assertThat(product.isOnPromotion()).isFalse();
        assertThat(product.getEffectivePrice()).isEqualByComparingTo("100.00");
        assertThat(product.getDiscountRatio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void withoutPrice_isNotOnPromotion() {
        Product product = product(null, "10.00");

        assertThat(product.isOnPromotion()).isFalse();
        assertThat(product.getEffectivePrice()).isNull();
    }

    @Test
    void discountRatio_isRoundedToFourDecimals() {
        Product product = product("3.00", "2.00");

        assertThat(product.getDiscountRatio()).isEqualByComparingTo("0.3333");
    }

    @Test
    void serialization_exposesPromotionFieldsButNotDiscountRatio() {
        JsonNode json = new ObjectMapper().valueToTree(product("100.00", "80.00"));

        assertThat(json.get("promotionalPrice").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(json.get("effectivePrice").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(json.get("onPromotion").booleanValue()).isTrue();
        assertThat(json.has("discountRatio")).isFalse();
    }
}
