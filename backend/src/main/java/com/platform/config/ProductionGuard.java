package com.platform.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
        System.out.println("\n========== PRODUCTION CONFIGURATION CHECK ==========");

        boolean hasError = false;

        if (debug) {
            System.err.println("❌ DEBUG MODE IS ENABLED IN PRODUCTION! Set debug=false");
            hasError = true;
        }

        if (showSql) {
            System.err.println("❌ SQL SHOW ENABLED IN PRODUCTION! Set spring.jpa.show-sql=false");
            hasError = true;
        }

        if (!"never".equals(stacktrace)) {
            System.err.println("❌ STACKTRACE EXPOSED IN PRODUCTION! Set server.error.include-stacktrace=never");
            hasError = true;
        }

        if (h2ConsoleEnabled) {
            System.err.println("❌ H2 CONSOLE ENABLED IN PRODUCTION! Set spring.h2.console.enabled=false");
            hasError = true;
        }

        // Проверка на опасные эндпоинты
        for (String endpoint : FORBIDDEN_ENDPOINTS) {
            if (exposedEndpoints.contains(endpoint)) {
                System.err.println("❌ DANGEROUS ENDPOINT EXPOSED: " + endpoint +
                    "! Remove from management.endpoints.web.exposure.include");
                hasError = true;
            }
        }

        if (hasError) {
            System.err.println("====================================================\n");
            throw new IllegalStateException("Production configuration is unsafe. Fix the issues above.");
        }

        System.out.println("✅ Production configuration is safe");
        System.out.println("   - Debug mode: OFF");
        System.out.println("   - SQL logging: OFF");
        System.out.println("   - Stacktraces: HIDDEN");
        System.out.println("   - H2 Console: OFF");
        System.out.println("====================================================\n");
    }
}