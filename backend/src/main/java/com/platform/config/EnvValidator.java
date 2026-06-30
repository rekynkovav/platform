package com.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EnvValidator {

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    @Value("${spring.datasource.username:}")
    private String dbUser;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @PostConstruct
    public void validate() {
        log.info("Validating environment for profile: {}", activeProfile);
        log.debug("DB URL resolved to: {}", dbUrl);
        log.debug("DB User resolved to: {}", dbUser);

        StringBuilder errors = new StringBuilder();

        if (dbUrl == null || dbUrl.isBlank()) {
            errors.append("DATABASE_URL (spring.datasource.url) is not configured!\n");
        }
        if (dbUser == null || dbUser.isBlank()) {
            errors.append("DATABASE_USER (spring.datasource.username) is not configured!\n");
        }
        if (dbPassword == null || dbPassword.isBlank()) {
            errors.append("DATABASE_PASSWORD (spring.datasource.password) is not configured!\n");
        }

        if (errors.length() > 0) {
            log.error("========== ENVIRONMENT VALIDATION FAILED ==========");
            log.error("Active profile: {}", activeProfile);
            log.error("Resolved DB URL: '{}'", dbUrl);
            log.error("Resolved DB User: '{}'", dbUser);
            log.error("Errors:\n{}", errors);
            log.error("===================================================");
            throw new IllegalStateException("Missing required environment variables. Check logs above.");
        }

        log.info("✅ All required environment variables are present for profile '{}'", activeProfile);
    }
}
