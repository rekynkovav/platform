package com.platform.performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoadTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testConcurrentHealthRequests() throws InterruptedException {
        int threads = 50;
        int requestsPerThread = 100;
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(2, TimeUnit.MINUTES);

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("=== Load Test Results ===");
        System.out.println("Total requests: " + (threads * requestsPerThread));
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + errorCount.get());
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Requests per second: " + (threads * requestsPerThread * 1000.0 / duration));

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(errorCount.get()).isLessThan((threads * requestsPerThread) * 0.05); // менее 5% ошибок
    }
}