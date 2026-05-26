package com.platform.controller;

import com.platform.model.dto.CommentDto;
import com.platform.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(
            @PathVariable String postId,
            @RequestParam(defaultValue = "50") int limit) {

        List<CommentDto> comments = commentService.getCommentsByPost(postId, limit);
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @PathVariable String postId,
            @Valid @RequestBody CommentDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        CommentDto comment = commentService.createComment(postId, request, jwt);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId,
            @AuthenticationPrincipal Jwt jwt) {

        commentService.deleteComment(postId, commentId, jwt);
        return ResponseEntity.noContent().build();
    }
}
