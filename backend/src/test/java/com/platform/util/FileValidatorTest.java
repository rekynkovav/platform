package com.platform.util;

import com.platform.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator();
    }

    @Test
    void validateImage_ValidJpeg_ReturnsTrue() throws IOException {
        // Arrange
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}; // JPEG header
        MockMultipartFile file = new MockMultipartFile(
                "image.jpg",
                "image.jpg",
                "image/jpeg",
                jpegBytes
        );

        // Act
        boolean result = fileValidator.validateImage(file);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateImage_ValidPng_ReturnsTrue() throws IOException {
        // Arrange
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}; // PNG header
        MockMultipartFile file = new MockMultipartFile(
                "image.png",
                "image.png",
                "image/png",
                pngBytes
        );

        // Act
        boolean result = fileValidator.validateImage(file);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateImage_ValidGif_ReturnsTrue() throws IOException {
        // Arrange
        byte[] gifBytes = "GIF89a".getBytes(StandardCharsets.US_ASCII); // GIF header
        MockMultipartFile file = new MockMultipartFile(
                "image.gif",
                "image.gif",
                "image/gif",
                gifBytes
        );

        // Act
        boolean result = fileValidator.validateImage(file);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateImage_InvalidContentType_ThrowsException() throws IOException {
        // Arrange
        byte[] content = "fake image content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image.txt",
                "image.txt",
                "text/plain", // Invalid content type
                content
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateImage(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void validateImage_InvalidMagicBytes_ThrowsException() throws IOException {
        // Arrange
        byte[] invalidBytes = "not an image".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image.jpg",
                "image.jpg",
                "image/jpeg", // Correct content type but wrong magic bytes
                invalidBytes
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateImage(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid image format");
    }

    @Test
    void validateImage_FileTooLarge_ThrowsException() throws IOException {
        // Arrange
        byte[] largeFile = new byte[11 * 1024 * 1024]; // 11 MB > 10 MB limit
        MockMultipartFile file = new MockMultipartFile(
                "large.jpg",
                "large.jpg",
                "image/jpeg",
                largeFile
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateImage(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File too large");
    }

    @Test
    void validateImage_EmptyFile_ThrowsException() throws IOException {
        // Arrange
        byte[] emptyBytes = new byte[0];
        MockMultipartFile file = new MockMultipartFile(
                "empty.jpg",
                "empty.jpg",
                "image/jpeg",
                emptyBytes
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateImage(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    void validateImage_ValidSize_ReturnsTrue() throws IOException {
        // Arrange
        byte[] validSizeFile = new byte[5 * 1024 * 1024]; // 5 MB < 10 MB limit
        validSizeFile[0] = (byte) 0xFF; // Add JPEG magic bytes
        validSizeFile[1] = (byte) 0xD8;
        validSizeFile[2] = (byte) 0xFF;
        validSizeFile[3] = (byte) 0xE0;

        MockMultipartFile file = new MockMultipartFile(
                "valid.jpg",
                "valid.jpg",
                "image/jpeg",
                validSizeFile
        );

        // Act
        boolean result = fileValidator.validateImage(file);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateVideo_ValidMp4_ReturnsTrue() throws IOException {
        // Arrange
        byte[] mp4Bytes = new byte[]{
                0x00, 0x00, 0x00, 0x18, // Box size
                0x66, 0x74, 0x79, 0x70, // 'ftyp' box
                0x6D, 0x70, 0x34, 0x32  // 'mp42' brand
        };
        MockMultipartFile file = new MockMultipartFile(
                "video.mp4",
                "video.mp4",
                "video/mp4",
                mp4Bytes
        );

        // Act
        boolean result = fileValidator.validateVideo(file);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateVideo_ValidWebm_ReturnsTrue() throws IOException {
        // Arrange
        byte[] webmBytes = new byte[]{
                0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 // WebM header
        };
        MockMultipartFile file = new MockMultipartFile(
                "video.webm",
                "video.webm",
                "video/webm",
                webmBytes
        );

        // Act
        boolean result = fileValidator.validateVideo(file);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateVideo_InvalidContentType_ThrowsException() throws IOException {
        // Arrange
        byte[] content = "fake video content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "video.txt",
                "video.txt",
                "text/plain", // Invalid content type
                content
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateVideo(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void validateVideo_FileTooLarge_ThrowsException() throws IOException {
        // Arrange
        byte[] largeFile = new byte[101 * 1024 * 1024]; // 101 MB > 100 MB limit
        MockMultipartFile file = new MockMultipartFile(
                "large.mp4",
                "large.mp4",
                "video/mp4",
                largeFile
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateVideo(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File too large");
    }

    @Test
    void validateDocument_ValidPdf_ReturnsTrue() throws IOException {
        // Arrange
        byte[] pdfBytes = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile(
                "document.pdf",
                "document.pdf",
                "application/pdf",
                pdfBytes
        );

        // Act
        boolean result = fileValidator.validateDocument(file);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateDocument_ValidDocx_ReturnsTrue() throws IOException {
        // Arrange
        byte[] docxBytes = new byte[]{
                0x50, 0x4B, 0x03, 0x04 // ZIP header (DOCX is a ZIP file)
        };
        MockMultipartFile file = new MockMultipartFile(
                "document.docx",
                "document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        // Act
        boolean result = fileValidator.validateDocument(file);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateDocument_InvalidContentType_ThrowsException() throws IOException {
        // Arrange
        byte[] content = "fake document".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "document.exe",
                "document.exe",
                "application/x-msdownload", // Executable file
                content
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateDocument(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void validateDocument_FileTooLarge_ThrowsException() throws IOException {
        // Arrange
        byte[] largeFile = new byte[51 * 1024 * 1024]; // 51 MB > 50 MB limit
        MockMultipartFile file = new MockMultipartFile(
                "large.pdf",
                "large.pdf",
                "application/pdf",
                largeFile
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateDocument(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File too large");
    }

    @Test
    void validateFileName_ValidName_ReturnsTrue() {
        // Arrange
        String fileName = "my-document_2024.pdf";

        // Act
        boolean result = fileValidator.validateFileName(fileName);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateFileName_InvalidCharacters_ThrowsException() {
        // Arrange
        String fileName = "../../etc/passwd";

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateFileName(fileName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid file name");
    }

    @Test
    void validateFileName_TooLong_ThrowsException() {
        // Arrange
        String fileName = "a".repeat(256) + ".txt";

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateFileName(fileName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File name too long");
    }

    @Test
    void validateFileName_NoExtension_ThrowsException() {
        // Arrange
        String fileName = "filewithoutextension";

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateFileName(fileName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File must have an extension");
    }
}