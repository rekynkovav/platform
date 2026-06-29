package com.platform.integration.migration;

import com.platform.service.KeycloakRetryProcessor;
import com.platform.service.KeycloakService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for migration integration tests.
 * Manually starts Testcontainers PostgreSQL BEFORE Spring context loads.
 * Liquibase runs automatically on context startup.
 * Redis, Keycloak, JWT are mocked — we only test DB schema + migrations.
 */
@SpringBootTest
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractMigrationTest {

    static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startContainer() {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("platform_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Container must already be started when this is called
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // Mock external services not needed for migration testing
    @MockBean protected RedisTemplate<String, String> redisTemplate;
    @MockBean protected RedisTemplate<String, Object> bucketRedisTemplate;
    @MockBean protected RedisConnectionFactory redisConnectionFactory;
    @MockBean protected RedisMessageListenerContainer redisMessageListenerContainer;
    @MockBean protected KeycloakService keycloakService;
    @MockBean protected KeycloakRetryProcessor keycloakRetryProcessor;
    @MockBean protected JwtDecoder jwtDecoder;
}
