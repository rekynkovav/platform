package com.platform.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== CreateBroadcastRequest ====================

    @Test
    void createBroadcastRequest_Valid_PassesValidation() {
        CreateBroadcastRequest request = new CreateBroadcastRequest();
        request.setTitle("My Stream");
        request.setDescription("A test stream");
        request.setStreamType("video");

        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void createBroadcastRequest_BlankTitle_FailsValidation() {
        CreateBroadcastRequest request = new CreateBroadcastRequest();
        request.setTitle("");
        request.setDescription("desc");

        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("required"));
    }

    @Test
    void createBroadcastRequest_TitleTooLong_FailsValidation() {
        CreateBroadcastRequest request = new CreateBroadcastRequest();
        request.setTitle("a".repeat(201));
        request.setDescription("desc");

        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.iterator().next().getMessage().contains("200"));
    }

    @Test
    void createBroadcastRequest_DescriptionTooLong_FailsValidation() {
        CreateBroadcastRequest request = new CreateBroadcastRequest();
        request.setTitle("Valid Title");
        request.setDescription("d".repeat(2001));

        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.iterator().next().getMessage().contains("2000"));
    }

    @Test
    void createBroadcastRequest_NullDescription_PassesValidation() {
        CreateBroadcastRequest request = new CreateBroadcastRequest();
        request.setTitle("Valid");
        request.setDescription(null);

        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void createBroadcastRequest_DefaultStreamType_IsVideo() {
        CreateBroadcastRequest request = new CreateBroadcastRequest();
        request.setTitle("Test");

        assertEquals("video", request.getStreamType());
    }
}
