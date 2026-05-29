package com.platform.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private String avatarUrl;
    private String bio;
    private boolean banned;
    private String banReason;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @jakarta.validation.constraints.Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @jakarta.validation.constraints.Email(message = "Email should be valid")
        @jakarta.validation.constraints.Size(max = 100, message = "Email must not exceed 100 characters")
        private String email;

        @jakarta.validation.constraints.Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be a valid international number")
        @jakarta.validation.constraints.Size(max = 20, message = "Phone must not exceed 20 characters")
        private String phone;

        @jakarta.validation.constraints.Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        private String avatarUrl;

        @jakarta.validation.constraints.Size(max = 1000, message = "Bio must not exceed 1000 characters")
        private String bio;
    }
}
