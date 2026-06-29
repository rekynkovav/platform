package com.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.service.KeycloakRetryProcessor;
import com.platform.service.KeycloakService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Base class for integration tests.
 * Boots FULL Spring context with H2 + mocked Redis, Keycloak, JWT.
 * Imports IntegrationTestCacheConfig to override CacheConfig's RedisCacheManager.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(IntegrationTestCacheConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    // Mock all Redis/external beans
    @MockBean protected RedisTemplate<String, String> redisTemplate;
    @MockBean protected RedisTemplate<String, Object> bucketRedisTemplate;
    @MockBean protected RedisConnectionFactory redisConnectionFactory;
    @MockBean protected RedisMessageListenerContainer redisMessageListenerContainer;

    // Mock Keycloak
    @MockBean protected KeycloakService keycloakService;
    @MockBean protected KeycloakRetryProcessor keycloakRetryProcessor;

    // Mock JWT decoder
    @MockBean protected JwtDecoder jwtDecoder;

    @BeforeAll
    void setUpObjectMapper() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    protected Jwt createJwt(String userId, String username, String... roles) {
        var builder = Jwt.withTokenValue("test-token-" + UUID.randomUUID())
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("preferred_username", username)
                .claim("email", username + "@test.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        if (roles.length > 0) {
            builder.claim("realm_access", Map.of("roles", List.of(roles)));
        }
        return builder.build();
    }

    protected JwtAuthenticationToken createAuth(Jwt jwt, String... roleNames) {
        var authorities = java.util.Arrays.stream(roleNames)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities);
    }

    protected String userId() {
        return UUID.randomUUID().toString();
    }
}
