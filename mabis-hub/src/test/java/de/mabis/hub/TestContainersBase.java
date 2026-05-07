package de.mabis.hub;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Basisklasse für alle Spring-Boot-Integrationstests mit PostgreSQL.
 * Ein einziger Container wird für alle Subklassen gestartet (static).
 */
@Testcontainers
public abstract class TestContainersBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mabis")
            .withUsername("mabis")
            .withPassword("mabis");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",  () -> "");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "");
        registry.add("spring.autoconfigure.exclude",
            () -> "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet"
                + ".OAuth2ResourceServerAutoConfiguration");
    }

    // ── JWT-Hilfsmethoden für alle Tests ─────────────────────────────────────

    protected static JwtRequestPostProcessor alsVnb() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_VNB"));
    }

    protected static JwtRequestPostProcessor alsMsb() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_MSB"));
    }

    protected static JwtRequestPostProcessor alsBkv() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_BKV"));
    }

    protected static JwtRequestPostProcessor alsUnb() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_UNB"));
    }

    protected static JwtRequestPostProcessor alsAdmin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
