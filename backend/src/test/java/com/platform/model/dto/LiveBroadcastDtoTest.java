package com.platform.model.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LiveBroadcastDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void testJsonSerialization_DoesNotIncludeSensitiveFields() throws JsonProcessingException {
        LiveBroadcastDto dto = LiveBroadcastDto.builder()
                .id("broadcast-123")
                .title("Test Broadcast")
                .description("Test Description")
                .streamKey("super-secret-stream-key")
                .encryptionKey("super-secret-encryption-key")
                .isLive(true)
                .viewerCount(100)
                .startedAt(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);

        // Проверяем, что чувствительные поля НЕ включены в JSON
        assertThat(json).doesNotContain("super-secret-stream-key");
        assertThat(json).doesNotContain("super-secret-encryption-key");
        assertThat(json).doesNotContain("streamKey");
        assertThat(json).doesNotContain("encryptionKey");

        // Проверяем, что безопасные поля включены
        assertThat(json).contains("broadcast-123");
        assertThat(json).contains("Test Broadcast");
        assertThat(json).contains("Test Description");
        assertThat(json).contains("isLive");
        assertThat(json).contains("viewerCount");
    }

    @Test
    void testJsonDeserialization_IgnoresSensitiveFields() throws JsonProcessingException {
        String json = """
            {
                "id": "broadcast-123",
                "title": "Test Broadcast",
                "description": "Test Description",
                "streamKey": "should-be-ignored",
                "encryptionKey": "should-be-ignored-too",
                "isLive": true,
                "viewerCount": 100,
                "startedAt": "2024-01-01T12:00:00"
            }
            """;

        LiveBroadcastDto dto = objectMapper.readValue(json, LiveBroadcastDto.class);

        // Проверяем, что чувствительные поля null после десериализации
        assertThat(dto.getStreamKey()).isNull();
        assertThat(dto.getEncryptionKey()).isNull();

        // Проверяем, что безопасные поля правильно десериализованы
        assertThat(dto.getId()).isEqualTo("broadcast-123");
        assertThat(dto.getTitle()).isEqualTo("Test Broadcast");
        assertThat(dto.getDescription()).isEqualTo("Test Description");
        assertThat(dto.isLive()).isTrue();
        assertThat(dto.getViewerCount()).isEqualTo(100);
        assertThat(dto.getStartedAt()).isNotNull();
    }

    @Test
    void testBuilder_CreatesValidDto() {
        LocalDateTime now = LocalDateTime.now();

        LiveBroadcastDto dto = LiveBroadcastDto.builder()
                .id("test-id")
                .title("Test Title")
                .description("Test Description")
                .streamKey("secret-key")
                .encryptionKey("enc-key")
                .isLive(true)
                .viewerCount(50)
                .startedAt(now)
                .build();

        assertThat(dto.getId()).isEqualTo("test-id");
        assertThat(dto.getTitle()).isEqualTo("Test Title");
        assertThat(dto.getDescription()).isEqualTo("Test Description");
        assertThat(dto.getStreamKey()).isEqualTo("secret-key");
        assertThat(dto.getEncryptionKey()).isEqualTo("enc-key");
        assertThat(dto.isLive()).isTrue();
        assertThat(dto.getViewerCount()).isEqualTo(50);
        assertThat(dto.getStartedAt()).isEqualTo(now);
    }

    @Test
    void testEqualsAndHashCode() {
        LiveBroadcastDto dto1 = LiveBroadcastDto.builder()
                .id("same-id")
                .title("Title 1")
                .build();

        LiveBroadcastDto dto2 = LiveBroadcastDto.builder()
                .id("same-id")
                .title("Title 2") // Разный title, но одинаковый id
                .build();

        LiveBroadcastDto dto3 = LiveBroadcastDto.builder()
                .id("different-id")
                .title("Title 1")
                .build();

        // equals должен сравнивать по id
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);

        // hashCode должен быть одинаковым для одинаковых id
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }
}