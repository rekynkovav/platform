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
import jakarta.validation.Valid;

import java.util.List;

/**
 * Контроллер ленты новостей для фронтенда.
 * Делегирует вызовы в существующий PostService.
 *
 * Frontend вызывает:
 *   GET  /api/feed/main
 *   GET  /api/feed/nutrition
 *   POST /api/feed/posts
 *   PUT  /api/feed/posts/{id}
 *   DELETE /api/feed/posts/{id}
 */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final PostService postService;

    /**
     * Основная лента
     * Frontend: api.get('/feed/main')
     */
    @GetMapping("/main")
    public ResponseEntity<List<PostDto>> getMainFeed(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {

        Pageable pageable = PageRequest.of(0, limit);
        List<PostDto> feed = postService.getFeed(jwt.getSubject(), before, pageable);
        return ResponseEntity.ok(feed);
    }

    /**
     * Лента питания — фильтрует посты по категории "nutrition"
     * Frontend: api.get('/feed/nutrition')
     */
    @GetMapping("/nutrition")
    public ResponseEntity<List<PostDto>> getNutritionFeed(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {

        Pageable pageable = PageRequest.of(0, limit);
        List<PostDto> feed = postService.getFeedByCategory("nutrition", jwt.getSubject(), before, pageable);
        return ResponseEntity.ok(feed);
    }

    /**
     * Создание поста через /feed/posts
     * Frontend: api.post('/feed/posts', data)
     */
    @PostMapping("/posts")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<PostDto> createPost(
            @Valid @RequestBody PostDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PostDto post = postService.createPost(request, jwt);
        return ResponseEntity.ok(post);
    }

    /**
     * Обновление поста через /feed/posts/{id}
     * Frontend: api.put(`/feed/posts/${id}`, data)
     */
    @PutMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable String id,
            @Valid @RequestBody PostDto.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PostDto post = postService.updatePost(id, request, jwt);
        return ResponseEntity.ok(post);
    }

    /**
     * Удаление поста через /feed/posts/{id}
     * Frontend: api.delete(`/feed/posts/${id}`)
     */
    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<Void> deletePost(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        postService.deletePost(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
