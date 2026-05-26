package com.platform.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DiskSpaceHealthIndicator implements HealthIndicator {

    private static final long MIN_FREE_SPACE = 100 * 1024 * 1024; // 100MB

    @Override
    public Health health() {
        File disk = new File(".");
        long freeSpace = disk.getFreeSpace();
        long totalSpace = disk.getTotalSpace();
        long usedSpace = totalSpace - freeSpace;
        double usedPercentage = totalSpace > 0 ? (usedSpace * 100.0 / totalSpace) : 0;

        if (freeSpace < MIN_FREE_SPACE) {
            return Health.down()
                    .withDetail("disk", "low space")
                    .withDetail("freeSpace", formatBytes(freeSpace))
                    .withDetail("totalSpace", formatBytes(totalSpace))
                    .withDetail("usedPercentage", String.format("%.1f%%", usedPercentage))
                    .withDetail("minimumRequired", formatBytes(MIN_FREE_SPACE))
                    .build();
        }

        return Health.up()
                .withDetail("disk", "healthy")
                .withDetail("freeSpace", formatBytes(freeSpace))
                .withDetail("totalSpace", formatBytes(totalSpace))
                .withDetail("usedPercentage", String.format("%.1f%%", usedPercentage))
                .build();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}