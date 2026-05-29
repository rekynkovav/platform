package com.platform.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    @TempDir
    Path tempDir;

    private byte[] createJpegMagicBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    }

    private byte[] createPngMagicBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private byte[] createPdfMagicBytes() {
        return new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    }

    @Test
    void testValidate_RealJpegFile_PassesValidation() throws IOException {
        byte[] jpegData = createJpegMagicBytes();
        MockMultipartFile file = new MockMultipartFile(
                "test.jpg", "test.jpg", "image/jpeg", jpegData);

        assertThat(FileValidator.validate(file)).isTrue();
    }

    @Test
    void testValidate_RealPngFile_PassesValidation() throws IOException {
        byte[] pngData = createPngMagicBytes();
        MockMultipartFile file = new MockMultipartFile(
                "test.png", "test.png", "image/png", pngData);

        assertThat(FileValidator.validate(file)).isTrue();
    }

    @Test
    void testValidate_RealPdfFile_PassesValidation() throws IOException {
        byte[] pdfData = createPdfMagicBytes();
        MockMultipartFile file = new MockMultipartFile(
                "test.pdf", "test.pdf", "application/pdf", pdfData);

        assertThat(FileValidator.validate(file)).isTrue();
    }

    @Test
    void testValidate_ExeWithPngContentType_FailsValidation() throws IOException {
        // EXE файл с магическими байтами MZ
        byte[] exeData = new byte[]{0x4D, 0x5A, 0x90, 0x00}; // MZ header
        MockMultipartFile file = new MockMultipartFile(
                "malware.exe", "malware.exe", "image/png", exeData);

        assertThat(FileValidator.validate(file)).isFalse();
    }

    @Test
    void testValidate_TextFileWithJpegContentType_FailsValidation() throws IOException {
        byte[] textData = "This is plain text, not a JPEG".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "fake.jpg", "fake.jpg", "image/jpeg", textData);

        assertThat(FileValidator.validate(file)).isFalse();
    }

    @Test
    void testValidate_EmptyFile_FailsValidation() {
        MockMultipartFile file = new MockMultipartFile(
                "empty.jpg", "empty.jpg", "image/jpeg", new byte[0]);

        assertThat(FileValidator.validate(file)).isFalse();
    }

    @Test
    void testValidate_NullFile_FailsValidation() {
        assertThat(FileValidator.validate(null)).isFalse();
    }

    @Test
    void testValidate_FileTooLarge_FailsValidation() throws IOException {
        // Создаем файл больше 10MB (лимит из P0-4)
        byte[] largeData = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "large.jpg", "large.jpg", "image/jpeg", largeData);

        assertThat(FileValidator.validate(file)).isFalse();
    }

    @Test
    void testValidate_ValidSizeFile_PassesValidation() throws IOException {
        byte[] jpegData = createJpegMagicBytes();
        MockMultipartFile file = new MockMultipartFile(
                "normal.jpg", "normal.jpg", "image/jpeg", jpegData);

        assertThat(FileValidator.validate(file)).isTrue();
    }

    @Test
    void testValidate_UnsupportedContentType_FailsValidation() throws IOException {
        byte[] data = createJpegMagicBytes();
        MockMultipartFile file = new MockMultipartFile(
                "test.exe", "test.exe", "application/x-msdownload", data);

        assertThat(FileValidator.validate(file)).isFalse();
    }

    @Test
    void testValidate_NoExtensionButValidMagicBytes_PassesValidation() throws IOException {
        byte[] jpegData = createJpegMagicBytes();
        MockMultipartFile file = new MockMultipartFile(
                "test", "test", "image/jpeg", jpegData);

        assertThat(FileValidator.validate(file)).isTrue();
    }

    @Test
    void testValidate_WrongExtensionButValidMagicBytes_PassesValidation() throws IOException {
        byte[] jpegData = createJpegMagicBytes();
        MockMultipartFile file = new MockMultipartFile(
                "test.txt", "test.txt", "image/jpeg", jpegData);

        // Magic bytes важнее расширения
        assertThat(FileValidator.validate(file)).isTrue();
    }

    @Test
    void testGetAllowedMimeTypes_ReturnsCorrectList() {
        var allowedTypes = FileValidator.getAllowedMimeTypes();

        assertThat(allowedTypes)
                .contains("image/jpeg", "image/png", "image/gif", "application/pdf")
                .doesNotContain("application/x-msdownload", "text/html");
    }

    @Test
    void testGetMaxFileSize_Returns10MB() {
        assertThat(FileValidator.getMaxFileSize()).isEqualTo(10 * 1024 * 1024L);
    }
}