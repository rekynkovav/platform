package com.platform.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvValidator {

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    @Value("${spring.datasource.username:}")
    private String dbUser;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.data.redis.host:}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private static final String[] REQUIRED_VARS = {
        "spring.datasource.url",
        "spring.datasource.username",
        "spring.datasource.password",
        "spring.data.redis.host"
    };

    @PostConstruct
    public void validate() {
        StringBuilder errors = new StringBuilder();

        if (dbUrl == null || dbUrl.isBlank()) {
            errors.append("❌ DATABASE_URL (spring.datasource.url) is not configured!\n");
        }
        if (dbUser == null || dbUser.isBlank()) {
            errors.append("❌ DATABASE_USER (spring.datasource.username) is not configured!\n");
        }
        if (dbPassword == null || dbPassword.isBlank()) {
            errors.append("❌ DATABASE_PASSWORD (spring.datasource.password) is not configured!\n");
        }
        if (redisHost == null || redisHost.isBlank()) {
            errors.append("❌ REDIS_HOST (spring.data.redis.host) is not configured!\n");
        }

        if (errors.length() > 0) {
            System.err.println("\n========== ENVIRONMENT VALIDATION FAILED ==========");
            System.err.print(errors.toString());
            System.err.println("===================================================\n");
            throw new IllegalStateException("Missing required environment variables");
        }

        System.out.println("✅ All required environment variables are present:");
        System.out.println("   - Database: " + dbUrl);
        System.out.println("   - Redis: " + redisHost + ":" + redisPort);
    }
}