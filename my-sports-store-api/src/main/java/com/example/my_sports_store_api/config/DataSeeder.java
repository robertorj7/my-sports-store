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
                product("Bola de Futebol de Campo Pro", "Bola oficial de campo com padrão FIFA Quality, costura térmica e excelente controle de toque. Ideal para partidas e treinos.", "199.90", "Futebol", "Branco", "https://images.unsplash.com/photo-1614632537197-38a17061c2bd", 50),
                product("Bola de Futebol Society", "Bola para gramado sintético com câmara de butil e revestimento em PU resistente à abrasão.", "129.90", "Futebol", "Colorido", "https://images.unsplash.com/photo-1579952363873-27f3bade9f55", 45),
                product("Chuteira de Campo Velocity", "Chuteira leve com cabedal sintético e travas para gramado natural, pensada para velocidade e explosão.", "349.90", "Futebol", "Preto", "https://images.unsplash.com/photo-1560272564-c83b66b1ad12", 30),
                product("Tênis de Corrida Performance", "Tênis responsivo com entressola em espuma de alto retorno e cabedal em mesh respirável para treinos diários.", "499.90", "Corrida", "Vermelho", "https://images.unsplash.com/photo-1542291026-7eec264c27ff", 35),
                product("Tênis de Corrida Amortecido", "Máximo amortecimento para longas distâncias, com solado de borracha durável e ótima estabilidade.", "599.90", "Corrida", "Branco", "https://images.unsplash.com/photo-1600185365483-26d7a4cc7519", 30),
                product("Tênis de Corrida Leve", "Modelo leve e flexível com ajuste tipo meia, ideal para treinos de ritmo e provas de 5 km a 21 km.", "449.90", "Corrida", "Preto", "https://images.unsplash.com/photo-1508609349937-5ec4ae374ebf", 40),
                product("Relógio GPS de Corrida", "Relógio esportivo com GPS integrado, monitor cardíaco no pulso e bateria de até 14 dias.", "1299.00", "Corrida", "Branco", "https://images.unsplash.com/photo-1523275335684-37898b6baf30", 15),
                product("Pochete de Corrida", "Pochete ajustável e antibalanço com bolso para celular e chaves, em tecido leve e respirável.", "79.90", "Corrida", "Preto", "https://images.unsplash.com/photo-1594882645126-14020914d58d", 60),
                product("Sapatilha de Atletismo com Cravos", "Sapatilha para pista com placa de cravos removíveis, indicada para provas de velocidade.", "399.90", "Corrida", "Preto", "https://images.unsplash.com/photo-1461896836934-ffe607ba8211", 20),
                product("Raquete de Tênis Carbono", "Raquete de competição com quadro em fibra de carbono, 300 g, oferecendo potência e controle.", "1199.90", "Tênis", "Vermelho", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6", 20),
                product("Tubo de Bolas de Tênis", "Tubo pressurizado com 3 bolas de feltro premium, aprovadas para todos os tipos de quadra.", "69.90", "Tênis", "Amarelo", "https://images.unsplash.com/photo-1587280501635-68a0e82cd5ff", 80),
                product("Tênis para Quadra Clay", "Calçado com solado espinha de peixe para quadras de saibro, oferecendo aderência e suporte lateral.", "549.90", "Tênis", "Branco", "https://images.unsplash.com/photo-1608231387042-66d1773070a5", 25),
                product("Bola de Basquete Indoor/Outdoor", "Bola tamanho 7 em couro composto, com excelente pegada para quadras cobertas e externas.", "179.90", "Basquete", "Laranja", "https://images.unsplash.com/photo-1519861531473-9200262188bf", 40),
                product("Tênis de Basquete Cano Alto", "Tênis de cano alto com proteção de tornozelo e amortecimento para saltos e mudanças de direção.", "899.90", "Basquete", "Vermelho", "https://images.unsplash.com/photo-1575537302964-96cd47c06b1b", 20),
                product("Aro de Basquete com Rede", "Aro oficial em aço maciço de 45 cm com rede de nylon e molas de retorno, para instalação em tabela.", "449.90", "Basquete", "Laranja", "https://images.unsplash.com/photo-1546519638-68e109498ffc", 12),
                product("Bola de Vôlei de Praia", "Bola costurada à mão, resistente à água e à areia, com toque macio para o vôlei de praia.", "159.90", "Vôlei", "Amarelo", "https://images.unsplash.com/photo-1592656094267-764a45160876", 35),
                product("Kit de Halteres Ajustáveis", "Par de halteres ajustáveis de 2,5 kg a 24 kg com troca rápida de carga, ideal para academia em casa.", "1499.90", "Fitness", "Cinza", "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61", 15),
                product("Banco de Supino Regulável", "Banco com encosto regulável em 7 posições e estrutura reforçada que suporta até 250 kg.", "899.90", "Fitness", "Preto", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b", 10),
                product("Barra Olímpica 20 kg", "Barra olímpica de 2,20 m em aço cromado com rolamentos, para agachamento, supino e levantamentos.", "799.90", "Fitness", "Prata", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438", 12),
                product("Tapete de Yoga Pro", "Tapete antiderrapante de 6 mm com alça para transporte, confortável para yoga e pilates.", "189.90", "Fitness", "Roxo", "https://images.unsplash.com/photo-1592432678016-e910b452f9a2", 60),
                product("Corda Naval 12 m", "Corda de treinamento funcional com 38 mm de diâmetro e pegadas emborrachadas.", "299.90", "Fitness", "Preto", "https://images.unsplash.com/photo-1599058917212-d750089bc07e", 18),
                product("Kit de Elásticos de Resistência", "Kit com 5 elásticos de intensidades diferentes, alças, tornozeleiras e âncora de porta.", "129.90", "Fitness", "Colorido", "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2", 50),
                product("Bicicleta Ergométrica Spinning", "Bike de spinning com volante de 13 kg, regulagem de carga e banco ajustável para treinos intensos.", "2499.90", "Fitness", "Preto", "https://images.unsplash.com/photo-1598289431512-b97b0917affc", 8),
                product("Tênis de Treino Cross", "Tênis estável com base firme e reforço lateral, ideal para musculação, cross training e HIIT.", "529.90", "Fitness", "Verde", "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa", 30),
                product("Bicicleta Speed Carbono", "Bicicleta de estrada com quadro em carbono, grupo de 22 velocidades e freios a disco hidráulicos.", "8999.90", "Ciclismo", "Preto", "https://images.unsplash.com/photo-1576435728678-68d0fbf94e91", 5),
                product("Bicicleta Fixa Urbana", "Bicicleta fixa com quadro em aço, cubo flip-flop e visual clássico para o dia a dia na cidade.", "2299.90", "Ciclismo", "Branco", "https://images.unsplash.com/photo-1485965120184-e220f721d03e", 8),
                product("Capacete de Ciclismo Aero", "Capacete aerodinâmico com 18 entradas de ventilação e regulagem de ajuste traseira.", "349.90", "Ciclismo", "Azul", "https://images.unsplash.com/photo-1517649763962-0c623066013b", 25),
                product("Óculos de Natação Pro", "Óculos de competição com lentes antiembaçantes, proteção UV e vedação em silicone macio.", "89.90", "Natação", "Transparente", "https://images.unsplash.com/photo-1530549387789-4c1017266635", 70),
                product("Garrafa Térmica Inox 750 ml", "Garrafa de aço inox com parede dupla que mantém a bebida gelada por até 24 horas.", "119.90", "Acessórios", "Verde", "https://images.unsplash.com/photo-1602143407151-7111542de6e8", 65),
                product("Mochila Esportiva 25 L", "Mochila resistente à água com compartimento para notebook, bolso para calçados e alças acolchoadas.", "229.90", "Acessórios", "Azul-marinho", "https://images.unsplash.com/photo-1553062407-98eeb64c6a62", 40)
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
