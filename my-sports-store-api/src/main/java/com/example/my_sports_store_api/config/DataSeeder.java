package com.example.my_sports_store_api.config;

import com.example.my_sports_store_api.model.Product;
import com.example.my_sports_store_api.model.Role;
import com.example.my_sports_store_api.model.User;
import com.example.my_sports_store_api.repository.ProductRepository;
import com.example.my_sports_store_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedProducts();
        seedAdmin();
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.saveAll(List.of(
                product("Pro Match Soccer Ball", "FIFA-quality soccer ball for match play.", "29.99", "Soccer", "White", "https://images.unsplash.com/photo-1614632537197-38a17061c2bd", 50),
                product("Trail Running Shoes", "Lightweight shoes with grip for off-road running.", "89.99", "Running", "Black", "https://images.unsplash.com/photo-1542291026-7eec264c27ff", 35),
                product("Carbon Tennis Racket", "Tournament-grade racket with carbon frame.", "149.99", "Tennis", "Red", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6", 20),
                product("Basketball Indoor/Outdoor", "Composite leather basketball, size 7.", "24.99", "Basketball", "Orange", "https://images.unsplash.com/photo-1519861531473-9200262188bf", 40),
                product("Adjustable Dumbbell Set", "5-25kg adjustable dumbbells for home gym.", "199.99", "Fitness", "Gray", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b", 15),
                product("Yoga Mat Pro", "Non-slip 6mm yoga mat with carry strap.", "34.99", "Fitness", "Purple", "https://images.unsplash.com/photo-1592432678016-e910b452f9a2", 60),
                product("Cycling Helmet Aero", "Aerodynamic helmet with adjustable fit.", "74.99", "Cycling", "Blue", "https://images.unsplash.com/photo-1557803175-3c2f6f42e0b8", 25),
                product("Swim Goggles Pro", "Anti-fog racing goggles with UV protection.", "19.99", "Swimming", "Clear", "https://images.unsplash.com/photo-1560089168-6516aefc9bfc", 70)
        ));
    }

    private void seedAdmin() {
        if (userRepository.existsByEmailIgnoreCase("admin@sportsstore.com")) {
            return;
        }

        userRepository.save(User.builder()
                .email("admin@sportsstore.com")
                .password(passwordEncoder.encode("Admin123!"))
                .name("Store Admin")
                .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER))
                .build());
    }

    private Product product(String name, String description, String price, String category, String color, String image, int stock) {
        return new Product(null, name, description, new BigDecimal(price), image, color, category, stock);
    }
}
