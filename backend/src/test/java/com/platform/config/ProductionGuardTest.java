package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProductionGuardTest {

    private ProductionGuard createGuard(boolean debug, boolean showSql, String stacktrace,
                                         String exposedEndpoints, boolean h2Console) {
        ProductionGuard guard = new ProductionGuard();
        ReflectionTestUtils.setField(guard, "debug", debug);
        ReflectionTestUtils.setField(guard, "showSql", showSql);
        ReflectionTestUtils.setField(guard, "stacktrace", stacktrace);
        ReflectionTestUtils.setField(guard, "exposedEndpoints", exposedEndpoints);
        ReflectionTestUtils.setField(guard, "h2ConsoleEnabled", h2Console);
        return guard;
    }

    @Test
    void validateProductionConfig_SafeConfig_DoesNotThrow() {
        ProductionGuard guard = createGuard(false, false, "never", "health,info", false);
        assertDoesNotThrow(guard::validateProductionConfig);
    }

    @Test
    void validateProductionConfig_DebugEnabled_Throws() {
        ProductionGuard guard = createGuard(true, false, "never", "health", false);
        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validateProductionConfig);
        assertTrue(ex.getMessage().contains("Production configuration is unsafe"));
    }

    @Test
    void validateProductionConfig_ShowSqlEnabled_Throws() {
        ProductionGuard guard = createGuard(false, true, "never", "health", false);
        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void validateProductionConfig_StacktraceExposed_Throws() {
        ProductionGuard guard = createGuard(false, false, "always", "health", false);
        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void validateProductionConfig_H2ConsoleEnabled_Throws() {
        ProductionGuard guard = createGuard(false, false, "never", "health", true);
        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }

    @Test
    void validateProductionConfig_DangerousEndpointExposed_Throws() {
        ProductionGuard guard = createGuard(false, false, "never", "health,env,info", false);
        assertThrows(IllegalStateException.class, guard::validateProductionConfig);
    }
}
