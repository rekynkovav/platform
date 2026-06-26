package com.platform.controller;

import com.platform.model.dto.PostDto;
import com.platform.service.PostService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/feed")
    public ResponseEntity<List<PostDto>> getFeed(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {

        Pageable pageable = PageRequest.of(0, limit);
        List<PostDto> feed = postService.getFeed(jwt.getSubject(), before, pageable);
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        PostDto post = postService.getPost(id, jwt.getSubject());
        return ResponseEntity.ok(post);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "post-limit")
    public ResponseEntity<PostDto> createPost(
            @Valid @RequestBody PostDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PostDto post = postService.createPost(request, jwt);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "post-limit")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable String id,
            @Valid @RequestBody PostDto.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PostDto post = postService.updatePost(id, request, jwt);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "post-limit")
    public ResponseEntity<Void> deletePost(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        postService.deletePost(id, jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/media")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    @RateLimiting(name = "post-limit")
    public ResponseEntity<PostDto.MediaUploadResponse> uploadMedia(
            @RequestParam("media") MultipartFile media,
            @RequestParam(required = false) String type) {

        PostDto.MediaUploadResponse response = postService.uploadSecureMedia(media, type);
        return ResponseEntity.ok(response);
    }
}