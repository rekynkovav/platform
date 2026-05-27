package com.platform.model.dto;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
public class QuestionDto {
    private String id;
    private String question;
    private String answer;
    private String authorId;
    private String authorName;
    private String mode;
    private String recipientId;
    private boolean isAnswered;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;
    private String answeredBy;

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Question cannot be empty")
        @Size(max = 500, message = "Question too long (max 500 characters)")
        private String question;
    }

    @Data
    public static class AnswerRequest {
        @NotBlank(message = "Answer cannot be empty")
        @Size(max = 2000, message = "Answer too long (max 2000 characters)")
        private String answer;
    }
}