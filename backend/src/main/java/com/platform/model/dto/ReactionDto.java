// src/main/java/com/platform/model/dto/ReactionDto.java
package com.platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDto {
    private String postId;
    private String userId;
    private String type;
    private Long count;
    private Boolean userReacted;
}