package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
class ConfigValidationTest {

    // ==================== EnvValidator ====================

    @Test
    void envValidator_AllPresent_DoesNotThrow() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/test");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void envValidator_MissingDbUrl_ThrowsIllegalState() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void envValidator_BlankDbUser_ThrowsIllegalState() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/test");
        ReflectionTestUtils.setField(validator, "dbUser", null);
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void envValidator_BlankDbPassword_ThrowsIllegalState() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/test");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "   ");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    // ==================== ProductionGuard ====================

    @Test
    void productionGuard_SafeConfig_DoesNotThrow() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "health,info");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertDoesNotThrow(guard::validateProductionConfig);
    }

    @Test
    void productionGuard_DebugEnabled_ThrowsIllegalState() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", true);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "health");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void productionGuard_ShowSqlEnabled_ThrowsIllegalState() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", true);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "health");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void productionGuard_StacktraceExposed_ThrowsIllegalState() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "always");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "health");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void productionGuard_H2ConsoleEnabled_ThrowsIllegalState() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "health");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", true);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void productionGuard_DangerousEndpointExposed_ThrowsIllegalState() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "health,env,info");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }
}
