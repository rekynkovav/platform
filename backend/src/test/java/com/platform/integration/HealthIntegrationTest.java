package com.platform.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the full Spring Boot context boots successfully
 * with H2 + mocked Redis.
 */
class HealthIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("GET /api/health — full context health check")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    @DisplayName("Full context boot — all beans wired correctly")
    void fullContextBoot() throws Exception {
        // If context boots, all @Autowired dependencies resolved
        mockMvc.perform(get("/api/events/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
