package com.platform.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthIndicatorsTest {

    // ==================== DatabaseHealthIndicator ====================

    @Test
    void databaseHealth_WhenQuerySucceeds_ReturnsUp() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(jdbcTemplate);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("PostgreSQL", health.getDetails().get("database"));
        assertEquals("connected", health.getDetails().get("status"));
    }

    @Test
    void databaseHealth_WhenQueryFails_ReturnsDown() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new RuntimeException("Connection refused"));

        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(jdbcTemplate);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("disconnected", health.getDetails().get("status"));
        assertEquals("Connection refused", health.getDetails().get("error"));
    }

    // ==================== DiskSpaceHealthIndicator ====================

    @Test
    void diskSpaceHealth_ReturnsValidHealth() {
        DiskSpaceHealthIndicator indicator = new DiskSpaceHealthIndicator();
        Health health = indicator.health();

        assertNotNull(health.getStatus());
        assertNotNull(health.getDetails().get("freeSpace"));
        assertNotNull(health.getDetails().get("totalSpace"));
        assertNotNull(health.getDetails().get("usedPercentage"));
    }

    // ==================== RedisHealthIndicator ====================

    @Test
    void redisHealth_WhenPingReturnsPong_ReturnsUp() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(factory);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(connection.getClientName()).thenReturn("redis-server");

        RedisHealthIndicator indicator = new RedisHealthIndicator(redisTemplate);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("available", health.getDetails().get("redis"));
    }

    @Test
    void redisHealth_WhenExceptionThrown_ReturnsDown() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(factory);
        when(factory.getConnection()).thenThrow(new RuntimeException("Redis unavailable"));

        RedisHealthIndicator indicator = new RedisHealthIndicator(redisTemplate);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("unavailable", health.getDetails().get("redis"));
    }

    // ==================== AllServicesHealthIndicator ====================

    @Test
    void allServicesHealth_ContainsAllContributors() {
        RedisHealthIndicator redis = mock(RedisHealthIndicator.class);
        DatabaseHealthIndicator db = mock(DatabaseHealthIndicator.class);
        DiskSpaceHealthIndicator disk = mock(DiskSpaceHealthIndicator.class);

        AllServicesHealthIndicator composite = new AllServicesHealthIndicator(redis, db, disk);

        assertSame(redis, composite.getContributor("redis"));
        assertSame(db, composite.getContributor("database"));
        assertSame(disk, composite.getContributor("disk"));
        assertNull(composite.getContributor("nonexistent"));
    }

    @Test
    void allServicesHealth_IteratorReturnsThreeEntries() {
        RedisHealthIndicator redis = mock(RedisHealthIndicator.class);
        DatabaseHealthIndicator db = mock(DatabaseHealthIndicator.class);
        DiskSpaceHealthIndicator disk = mock(DiskSpaceHealthIndicator.class);

        AllServicesHealthIndicator composite = new AllServicesHealthIndicator(redis, db, disk);

        List<String> names = new ArrayList<>();
        for (NamedContributor<HealthContributor> nc : composite) {
            names.add(nc.getName());
        }

        assertEquals(3, names.size());
        assertTrue(names.contains("redis"));
        assertTrue(names.contains("database"));
        assertTrue(names.contains("disk"));
    }
}
