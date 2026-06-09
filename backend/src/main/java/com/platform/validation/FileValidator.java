package com.platform.validation;

import com.platform.exception.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FileValidator {

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList("video/mp4", "video/webm", "video/quicktime");
    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList("audio/mpeg", "audio/wav", "audio/ogg");
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList("application/pdf", "text/plain");

    private static final Map<String, List<byte[]>> MAGIC_BYTES = new HashMap<>();
    static {
        MAGIC_BYTES.put("image/jpeg", List.of(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        MAGIC_BYTES.put("image/png", List.of(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}));
        MAGIC_BYTES.put("image/gif", List.of(new byte[]{0x47, 0x49, 0x46, 0x38}));
        MAGIC_BYTES.put("image/webp", List.of(new byte[]{0x52, 0x49, 0x46, 0x46}));
        MAGIC_BYTES.put("video/mp4", List.of(new byte[]{0x00, 0x00, 0x00, (byte) 0x18, 0x66, 0x74, 0x79, 0x70}));
        MAGIC_BYTES.put("video/webm", List.of(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}));
        MAGIC_BYTES.put("application/pdf", List.of(new byte[]{0x25, 0x50, 0x44, 0x46}));
    }

    public void validate(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File size exceeds 100MB limit. Current size: " + file.getSize() + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new ValidationException("Could not determine file type");
        }

        switch (type) {
            case "image":
                if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                    throw new ValidationException("Invalid image type: " + contentType + ". Allowed: " + ALLOWED_IMAGE_TYPES);
                }
                break;
            case "video":
                if (!ALLOWED_VIDEO_TYPES.contains(contentType)) {
                    throw new ValidationException("Invalid video type: " + contentType + ". Allowed: " + ALLOWED_VIDEO_TYPES);
                }
                break;
            case "audio":
                if (!ALLOWED_AUDIO_TYPES.contains(contentType)) {
                    throw new ValidationException("Invalid audio type: " + contentType + ". Allowed: " + ALLOWED_AUDIO_TYPES);
                }
                break;
            case "document":
                if (!ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
                    throw new ValidationException("Invalid document type: " + contentType + ". Allowed: " + ALLOWED_DOCUMENT_TYPES);
                }
                break;
            default:
        }

        validateMagicBytes(file, contentType);
    }

    private void validateMagicBytes(MultipartFile file, String contentType) {
        List<byte[]> expectedMagicList = MAGIC_BYTES.get(contentType);
        if (expectedMagicList == null) {
            return;
        }

        byte[] fileHeader = new byte[8];
        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(fileHeader, 0, 8);
            if (bytesRead < 1) {
                throw new ValidationException("Could not read file header");
            }

            boolean match = false;
            for (byte[] magic : expectedMagicList) {
                if (bytesRead >= magic.length) {
                    boolean fullMatch = true;
                    for (int i = 0; i < magic.length; i++) {
                        if (fileHeader[i] != magic[i]) {
                            fullMatch = false;
                            break;
                        }
                    }
                    if (fullMatch) {
                        match = true;
                        break;
                    }
                }
            }

            if (!match) {
                throw new ValidationException("File content does not match expected type: " + contentType);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to validate file content: " + e.getMessage());
        }
    }
}