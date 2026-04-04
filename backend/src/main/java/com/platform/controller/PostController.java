package com.platform.controller;

import com.platform.model.dto.PostDto;
import com.platform.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PreAuthorize("hasAnyRole('admin', 'moderator')")
    public ResponseEntity<PostDto> createPost(
            @RequestBody PostDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PostDto post = postService.createPost(request, jwt);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'moderator')")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable String id,
            @RequestBody PostDto.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PostDto post = postService.updatePost(id, request, jwt);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'moderator')")
    public ResponseEntity<Void> deletePost(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        postService.deletePost(id, jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/media")
    @PreAuthorize("hasAnyRole('admin', 'moderator')")
    public ResponseEntity<PostDto.MediaUploadResponse> uploadMedia(
            @RequestParam("media") MultipartFile media,
            @RequestParam(required = false) String type) {

        PostDto.MediaUploadResponse response = postService.uploadMedia(media, type);
        return ResponseEntity.ok(response);
    }
}