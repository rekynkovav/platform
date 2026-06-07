package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EnvValidatorTest {

    @Test
    void validate_AllVarsPresent_DoesNotThrow() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost:5432/testdb");
        ReflectionTestUtils.setField(validator, "dbUser", "testuser");
        ReflectionTestUtils.setField(validator, "dbPassword", "testpass");
        ReflectionTestUtils.setField(validator, "redisHost", "localhost");
        ReflectionTestUtils.setField(validator, "redisPort", 6379);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_MissingDbUrl_ThrowsIllegalStateException() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");
        ReflectionTestUtils.setField(validator, "redisHost", "localhost");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("Missing required environment variables"));
    }

    @Test
    void validate_MissingRedisHost_ThrowsIllegalStateException() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/db");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");
        ReflectionTestUtils.setField(validator, "redisHost", null);

        assertThrows(IllegalStateException.class, validator::validate);
    }
}
