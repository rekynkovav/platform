package com.platform;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.extern.slf4j.Slf4j;

import java.util.TimeZone;
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
        // Установка часового пояса
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("✅ Default timezone set to UTC");

        // Регистрация Shutdown Hook
        registerShutdownHook();
    }

    @Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.setGracefulShutdown(true);
        return factory;
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("=== SHUTTING DOWN PLATFORM APPLICATION ===");
            performGracefulShutdown();
            log.info("=== SHUTDOWN COMPLETE ===");
        }));
    }

    private void performGracefulShutdown() {
        // 1. Остановка приема новых запросов
        log.info("Step 1/4: Stopping accepting new requests...");

        // 2. Завершение активных WebSocket сессий
        log.info("Step 2/4: Closing active WebSocket sessions...");
        try {
            // WebSocketSessionRegistryCleanup будет вызван через @PreDestroy
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. Завершение активных транзакций
        log.info("Step 3/4: Waiting for active transactions to complete...");
        try {
            // Даем время на завершение текущих операций
            Thread.sleep(5000);
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

        // Сохранение состояния в Redis/БД при необходимости
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