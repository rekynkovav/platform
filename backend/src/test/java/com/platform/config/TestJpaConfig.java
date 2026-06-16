package com.platform.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring configuration for @DataJpaTest repository tests.
 * Uses @Configuration (not @SpringBootApplication) to avoid triggering
 * broad auto-configuration that breaks the @DataJpaTest slice.
 * @DataJpaTest already handles datasource, JPA, and transaction management.
 * This config only adds entity scanning for the model package.
 */
@Configuration
@EntityScan(basePackages = "com.platform.model")
@EnableJpaRepositories(basePackages = "com.platform.repository")
public class TestJpaConfig {
}
