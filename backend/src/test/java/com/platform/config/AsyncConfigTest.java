package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void taskExecutor_ReturnsConfiguredExecutor() {
        Executor executor = config.taskExecutor();

        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertEquals(5, tpe.getCorePoolSize());
        assertEquals(20, tpe.getMaxPoolSize());
        assertEquals(500, tpe.getQueueCapacity());
    }

    @Test
    void ffmpegExecutor_ReturnsConfiguredExecutor() {
        Executor executor = config.ffmpegExecutor();

        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertEquals(10, tpe.getCorePoolSize());
        assertEquals(50, tpe.getMaxPoolSize());
        assertEquals(200, tpe.getQueueCapacity());
    }

    @Test
    void emailExecutor_ReturnsConfiguredExecutor() {
        Executor executor = config.emailExecutor();

        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertEquals(2, tpe.getCorePoolSize());
        assertEquals(10, tpe.getMaxPoolSize());
        assertEquals(100, tpe.getQueueCapacity());
    }
}
