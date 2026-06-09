package com.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
public class ProductionGuard {

    @Value("${debug:false}")
    private boolean debug;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Value("${server.error.include-stacktrace:never}")
    private String stacktrace;

    @Value("${management.endpoints.web.exposure.include:}")
    private String exposedEndpoints;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    private static final String[] FORBIDDEN_ENDPOINTS = {"env", "beans", "configprops", "mappings", "heapdump"};

    @PostConstruct
    public void validateProductionConfig() {
        log.info("\n========== PRODUCTION CONFIGURATION CHECK ==========");

        boolean hasError = false;

        if (debug) {
            log.error("DEBUG MODE IS ENABLED IN PRODUCTION! Set debug=false");
            hasError = true;
        }

        if (showSql) {
            log.error("SQL SHOW ENABLED IN PRODUCTION! Set spring.jpa.show-sql=false");
            hasError = true;
        }

        if (!"never".equals(stacktrace)) {
            log.error("STACKTRACE EXPOSED IN PRODUCTION! Set server.error.include-stacktrace=never");
            hasError = true;
        }

        if (h2ConsoleEnabled) {
            log.error("H2 CONSOLE ENABLED IN PRODUCTION! Set spring.h2.console.enabled=false");
            hasError = true;
        }

        // Проверка на опасные эндпоинты
        for (String endpoint : FORBIDDEN_ENDPOINTS) {
            if (exposedEndpoints.contains(endpoint)) {
                log.error("DANGEROUS ENDPOINT EXPOSED: {}! Remove from management.endpoints.web.exposure.include", endpoint);
                hasError = true;
            }
        }

        if (hasError) {
            log.error("====================================================\n");
            throw new IllegalStateException("Production configuration is unsafe. Fix the issues above.");
        }

        log.info("Production configuration is safe");
        log.info("  - Debug mode: OFF");
        log.info("  - SQL logging: OFF");
        log.info("  - Stacktraces: HIDDEN");
        log.info("  - H2 Console: OFF");
        log.info("====================================================\n");
    }
}