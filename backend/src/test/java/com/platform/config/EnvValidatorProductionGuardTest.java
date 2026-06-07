package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EnvValidatorProductionGuardTest {

    // ==================== EnvValidator ====================

    @Test
    void envValidator_AllVarsPresent_DoesNotThrow() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost:5432/db");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");
        ReflectionTestUtils.setField(validator, "redisHost", "localhost");
        ReflectionTestUtils.setField(validator, "redisPort", 6379);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void envValidator_MissingDbUrl_ThrowsIllegalStateException() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");
        ReflectionTestUtils.setField(validator, "redisHost", "localhost");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void envValidator_NullDbUser_ThrowsIllegalStateException() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/db");
        ReflectionTestUtils.setField(validator, "dbUser", null);
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");
        ReflectionTestUtils.setField(validator, "redisHost", "localhost");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void envValidator_BlankRedisHost_ThrowsIllegalStateException() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/db");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");
        ReflectionTestUtils.setField(validator, "redisHost", "   ");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void envValidator_MissingPassword_ThrowsIllegalStateException() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/db");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "");
        ReflectionTestUtils.setField(validator, "redisHost", "localhost");

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
    void productionGuard_DebugEnabled_ThrowsIllegalStateException() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", true);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void productionGuard_ShowSqlEnabled_ThrowsIllegalStateException() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", true);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void productionGuard_StacktraceExposed_ThrowsIllegalStateException() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "always");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void productionGuard_H2ConsoleEnabled_ThrowsIllegalStateException() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", true);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void productionGuard_DangerousEndpointExposed_ThrowsIllegalStateException() {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", false);
        ReflectionTestUtils.setField(guard, "showSql", false);
        ReflectionTestUtils.setField(guard, "stacktrace", "never");
        ReflectionTestUtils.setField(guard, "exposedEndpoints", "health,env,info");
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", false);

        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }
}
