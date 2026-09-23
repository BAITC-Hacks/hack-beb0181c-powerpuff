package com.powerpuff.backend;

import com.powerpuff.backend.entity.Product;
import com.powerpuff.backend.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EnabledIfSystemProperty(named = "postgresTests", matches = "true")
class PostgresMigrationTest {
    @Container static final PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
        r.add("app.ekt.enabled", () -> false);
    }
    @Autowired ProductRepository products;
    @Test void flywayAndHibernateAgreeOnPostgres() {
        products.saveAndFlush(new Product(515291L, "Test", "001_", "00012", new BigDecimal("0.01"), null));
        Product p = products.findById(515291L).orElseThrow();
        assertThat(p.getArticle()).isEqualTo("001_");
        assertThat(p.getPrice()).isEqualByComparingTo("0.01");
        assertThat(p.getQuantity()).isNull();
    }
}
