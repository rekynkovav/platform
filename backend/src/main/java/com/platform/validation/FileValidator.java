package com.platform.validation;

import com.platform.exception.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

@Component
public class FileValidator {

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList("video/mp4", "video/webm", "video/quicktime");
    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList("audio/mpeg", "audio/wav", "audio/ogg");
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList("application/pdf", "text/plain");

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
                // любой тип
        }
    }
}