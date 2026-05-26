package com.platform.monitoring;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

@Component
@Slf4j
public class ConnectionPoolMonitor {

    private final HikariDataSource dataSource;
    private final MeterRegistry meterRegistry;

    public ConnectionPoolMonitor(DataSource dataSource, MeterRegistry meterRegistry) {
        this.dataSource = (HikariDataSource) dataSource;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("ConnectionPoolMonitor initialized for pool: {}", dataSource.getPoolName());
    }

    @Scheduled(fixedRate = 30000)
    public void logPoolStats() {
        try {
            var pool = dataSource.getHikariPoolMXBean();
            int totalConnections = pool.getTotalConnections();
            int activeConnections = pool.getActiveConnections();
            int idleConnections = pool.getIdleConnections();
            int threadsAwaiting = pool.getThreadsAwaitingConnection();

            log.info("Connection Pool: total={}, active={}, idle={}, waiting={}",
                totalConnections, activeConnections, idleConnections, threadsAwaiting);

            meterRegistry.gauge("db.pool.total", totalConnections);
            meterRegistry.gauge("db.pool.active", activeConnections);
            meterRegistry.gauge("db.pool.idle", idleConnections);
            meterRegistry.gauge("db.pool.waiting", threadsAwaiting);

            if (threadsAwaiting > 10) {
                log.warn("⚠️ Connection pool starvation detected: {} threads waiting", threadsAwaiting);
            }
        } catch (Exception e) {
            log.error("Failed to get pool stats: {}", e.getMessage());
        }
    }
}