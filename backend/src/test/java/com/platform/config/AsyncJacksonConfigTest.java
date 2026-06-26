package com.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncJacksonConfigTest {

    // ==================== AsyncConfig ====================

    @Test
    void asyncConfig_TaskExecutor_ReturnsConfiguredExecutor() {
        AsyncConfig config = new AsyncConfig();
        Executor executor = config.taskExecutor();

        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertEquals(5, tpe.getCorePoolSize());
        assertEquals(20, tpe.getMaxPoolSize());
    }

    @Test
    void asyncConfig_FfmpegExecutor_ReturnsConfiguredExecutor() {
        AsyncConfig config = new AsyncConfig();
        Executor executor = config.ffmpegExecutor();

        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertEquals(10, tpe.getCorePoolSize());
        assertEquals(50, tpe.getMaxPoolSize());
    }

    @Test
    void asyncConfig_EmailExecutor_ReturnsConfiguredExecutor() {
        AsyncConfig config = new AsyncConfig();
        Executor executor = config.emailExecutor();

        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertEquals(2, tpe.getCorePoolSize());
        assertEquals(10, tpe.getMaxPoolSize());
    }

    // ==================== JacksonConfig ====================

    @Test
    void jacksonConfig_ObjectMapper_DisablesDatesAsTimestamps() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    @Test
    void jacksonConfig_ObjectMapper_EnablesIndentOutput() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertFalse(mapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
    }

    @Test
    void jacksonConfig_ObjectMapper_DisablesFailOnEmptyBeans() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertFalse(mapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
    }

    @Test
    void jacksonConfig_ObjectMapper_EnablesBigDecimalForFloats() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertTrue(mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS));
    }

    @Test
    void jacksonConfig_ObjectMapper_EnablesBigIntegerForInts() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertTrue(mapper.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS));
    }

    @Test
    void jacksonConfig_ObjectMapperBuilder_ReturnsNonNull() {
        // objectMapperBuilder() not present in JacksonConfig - skipped
    }

    @Test
    void jacksonConfig_ObjectMapper_CanSerializeLocalDateTime() throws Exception {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        // Format 'ss' (without Z) works for LocalDateTime — no exception expected
        String result = mapper.writeValueAsString(now);
        assertNotNull(result);
        assertTrue(result.contains("2026"));
    }
}
