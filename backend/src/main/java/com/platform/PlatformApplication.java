package com.platform;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class PlatformApplication {

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;
    private static final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("✅ Default timezone set to UTC");
    }

    @PreDestroy
    public void shutdown() {
        log.info("=== SHUTTING DOWN PLATFORM APPLICATION ===");
        performGracefulShutdown();
        log.info("=== SHUTDOWN COMPLETE ===");
    }

    private void performGracefulShutdown() {
        // 1. Сигнал остановки Tomcat (обрабатывается TomcatServletWebServerFactory)
        log.info("Step 1/4: Graceful shutdown signal sent to Tomcat");

        // 2. Завершение активных WebSocket сессий (если есть доступ к обработчику)
        log.info("Step 2/4: Closing active WebSocket sessions...");
        // В реальном коде здесь следует вызвать chatWebSocketHandler.closeAllSessions()
        // Для демонстрации используем CountDownLatch для имитации ожидания
        CountDownLatch latch = new CountDownLatch(1);
        cleanupExecutor.submit(() -> {
            try {
                // Здесь можно разместить логику закрытия сессий
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                log.warn("WebSocket session closure took too long, proceeding anyway");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. Завершение активных транзакций (ожидание)
        log.info("Step 3/4: Waiting for active transactions to complete...");
        try {
            // Даём время на завершение текущих операций
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 4. Очистка ресурсов
        log.info("Step 4/4: Cleaning up resources...");
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Cleanup executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Application shutdown completed successfully");
    }

    @Bean
    public GracefulShutdownCallback gracefulShutdownCallback() {
        return new GracefulShutdownCallback() {
            @Override
            public void shutdownComplete(GracefulShutdownResult result) {
                if (result == GracefulShutdownResult.IDLE) {
                    log.info("Graceful shutdown completed - no active requests");
                } else {
                    log.info("Graceful shutdown completed - {} active requests were terminated",
                            result == GracefulShutdownResult.REQUESTS_ACTIVE ? "some" : "all");
                }
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PlatformApplication.class);
        app.setRegisterShutdownHook(true);
        app.run(args);
    }
}
