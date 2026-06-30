package com.platform.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class StreamingFileServiceTest {

    @InjectMocks
    private StreamingFileService streamingFileService;

    // ==================== streamLargeFile ====================

    @Test
    void streamLargeFile_FullContent_CopiesCorrectly() throws IOException {
        byte[] data = "Hello, World! This is test data for streaming.".getBytes();
        InputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        streamingFileService.streamLargeFile(inputStream, outputStream, data.length);

        assertThat(outputStream.toByteArray()).isEqualTo(data);
    }

    @Test
    void streamLargeFile_ZeroSize_WritesNothing() throws IOException {
        byte[] data = "Some content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        streamingFileService.streamLargeFile(inputStream, outputStream, 0);

        assertThat(outputStream.toByteArray()).isEmpty();
    }

    @Test
    void streamLargeFile_PartialSize_CopiesOnlySpecifiedBytes() throws IOException {
        byte[] data = "Hello, World!".getBytes();
        InputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        streamingFileService.streamLargeFile(inputStream, outputStream, 5);

        assertThat(outputStream.toString()).isEqualTo("Hello");
    }

    @Test
    void streamLargeFile_LargeData_CopiesCorrectly() throws IOException {
        // Create a 100KB payload
        byte[] data = new byte[100 * 1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        InputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        streamingFileService.streamLargeFile(inputStream, outputStream, data.length);

        assertThat(outputStream.toByteArray()).isEqualTo(data);
    }

    @Test
    void streamLargeFile_EmptyInputStream_WritesNothing() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        streamingFileService.streamLargeFile(inputStream, outputStream, 100);

        assertThat(outputStream.toByteArray()).isEmpty();
    }

    // ==================== Constructor defaults ====================

    @Test
    void bufferSize_Is8KB() {
        Integer bufferSize = (Integer) ReflectionTestUtils.getField(streamingFileService, "BUFFER_SIZE");
        assertThat(bufferSize).isEqualTo(8192);
    }
}
