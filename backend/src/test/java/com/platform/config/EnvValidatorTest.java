package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
class EnvValidatorTest {

    @Test
    void validate_AllVarsPresent_DoesNotThrow() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/db");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_MissingDbUrl_ThrowsIllegalStateException() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "");
        ReflectionTestUtils.setField(validator, "dbUser", "user");
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("Missing required environment variables"));
    }

    @Test
    void validate_BlankDbUser_ThrowsIllegalStateException() {
        EnvValidator validator = new EnvValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost/db");
        ReflectionTestUtils.setField(validator, "dbUser", null);
        ReflectionTestUtils.setField(validator, "dbPassword", "pass");

        assertThrows(IllegalStateException.class, validator::validate);
    }
}
