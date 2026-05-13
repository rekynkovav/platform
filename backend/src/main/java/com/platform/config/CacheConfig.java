package com.platform.config;

import com.github.vladimir_bukhtoyarov.bucket4j.core.Bucket;
import com.github.vladimir_bukhtoyarov.bucket4j.core.Bucket4j;
import com.github.vladimir_bukhtoyarov.bucket4j.core.BucketConfiguration;
import com.github.vladimir_bukhtoyarov.bucket4j.core.limiter.Bandwidth;
import com.github.vladimir_bukhtoyarov.bucket4j.spring.boot.starter.Bucket4jConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)   // кэш живёт 5 минут
                .maximumSize(2000));                     // максимум 2000 записей
        return cacheManager;
    }

    /**
     * Глобальная конфигурация rate-limiting через Bucket4j
     */
    @Bean
    public Bucket4jConfiguration bucket4jConfiguration() {
        return new Bucket4jConfiguration() {
            @Override
            public BucketConfiguration getBucketConfiguration() {
                return BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))   // 100 запросов в минуту
                        .addLimit(Bandwidth.simple(500, Duration.ofHours(1)))    // 500 в час
                        .build();
            }
        };
    }

    @Bean
    public Bucket4jConfiguration chatBucketConfiguration() {
        return new Bucket4jConfiguration() {
            @Override
            public BucketConfiguration getBucketConfiguration() {
                return BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofMinutes(1)))
                        .addLimit(Bandwidth.simple(300, Duration.ofHours(1)))
                        .build();
            }
        };
    }

    @Bean
    public Bucket4jConfiguration mediaBucketConfiguration() {
        return new Bucket4jConfiguration() {
            @Override
            public BucketConfiguration getBucketConfiguration() {
                return BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(20, Duration.ofMinutes(1)))   // 20 запросов в минуту для медиа
                        .addLimit(Bandwidth.simple(100, Duration.ofHours(1)))    // 100 в час
                        .build();
            }
        };
    }

    @Bean
    public Bucket4jConfiguration notificationBucketConfiguration() {
        return new Bucket4jConfiguration() {
            @Override
            public BucketConfiguration getBucketConfiguration() {
                return BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(30, Duration.ofMinutes(1)))   // 30 запросов в минуту для уведомлений
                        .addLimit(Bandwidth.simple(150, Duration.ofHours(1)))    // 150 в час
                        .build();
            }
        };
    }
}