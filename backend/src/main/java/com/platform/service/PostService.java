package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Post;
import com.platform.model.dto.CommentDto;
import com.platform.model.dto.PostDto;
import com.platform.repository.PostRepository;
import com.platform.repository.ReactionRepository;
import com.platform.util.RoleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final CommentService commentService;
    private final SecureMediaService secureMediaService;

    public List<PostDto> getFeed(String userId, String before, Pageable pageable) {
        List<Post> posts;

        if (before != null && !before.isEmpty()) {
            posts = postRepository.findFeedBefore(before, pageable);
        } else {
            posts = postRepository.findFeed(pageable);
        }

        return posts.stream()
                .map(post -> toDto(post, userId))
                .collect(Collectors.toList());
    }

    public PostDto getPost(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        return toDto(post, userId);
    }

    @Transactional
    public PostDto createPost(PostDto.CreateRequest request, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String userId = jwt.getSubject();
        String userName = jwt.getClaim("preferred_username");
        String userRole = getUserRole(jwt);

        Post post = Post.builder()
                .id(UUID.randomUUID().toString())
                .content(request.getContent())
                .authorId(userId)
                .authorName(userName)
                .authorRole(userRole)
                .mediaUrl(request.getMediaUrl())
                .mediaType(request.getMediaType())
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .viewsCount(0)
                .flagged(false)
                .createdAt(LocalDateTime.now())
                .build();

        post = postRepository.save(post);
        log.info("Post created: {} by {}", post.getId(), userId);

        return toDto(post, userId);
    }

    @Transactional
    public PostDto updatePost(String postId, PostDto.UpdateRequest request, org.springframework.security.oauth2.jwt.Jwt jwt) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        if (!post.getAuthorId().equals(userId) && !isAdmin(userRole)) {
            throw new AccessDeniedException("You don't have permission to update this post");
        }

        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getMediaUrl() != null) post.setMediaUrl(request.getMediaUrl());
        if (request.getMediaType() != null) post.setMediaType(request.getMediaType());

        post.setUpdatedAt(LocalDateTime.now());
        post = postRepository.save(post);
        log.info("Post updated: {} by {}", postId, userId);

        return toDto(post, userId);
    }

    @Transactional
    public void deletePost(String postId, org.springframework.security.oauth2.jwt.Jwt jwt) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        String userId = jwt.getSubject();
        String userRole = getUserRole(jwt);

        if (!post.getAuthorId().equals(userId) && !isAdmin(userRole)) {
            throw new AccessDeniedException("You don't have permission to delete this post");
        }

        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
        log.info("Post deleted: {} by {}", postId, userId);
    }

    public PostDto.MediaUploadResponse uploadSecureMedia(MultipartFile file, String type) {
        try {
            String fileId = UUID.randomUUID().toString();
            // Получаем Jwt из контекста безопасности
            org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            Jwt jwt = (Jwt) authentication.getPrincipal();

            // Сохраняем файл через SecureMediaService
            com.platform.model.MediaFile mediaFile = secureMediaService.uploadSecureMedia(file, type, jwt, null);

            return PostDto.MediaUploadResponse.builder()
                .url(mediaFile.getS3Key())
                .type(mediaFile.getFileType())
                .size(mediaFile.getFileSize())
                .filename(mediaFile.getOriginalFilename())
                .build();
        } catch (Exception e) {
            log.error("Failed to upload media", e);
            throw new RuntimeException("Failed to upload media", e);
        }
    }

    private PostDto toDto(Post post, String userId) {
        boolean userLiked = reactionRepository.existsByPostIdAndUserIdAndType(post.getId(), userId, "like");

        List<CommentDto> recentComments = commentService.getRecentComments(post.getId(), 3);

        return PostDto.builder()
                .id(post.getId())
                .content(post.getContent())
                .authorId(post.getAuthorId())
                .authorName(post.getAuthorName())
                .authorRole(post.getAuthorRole())
                .mediaUrl(post.getMediaUrl())
                .mediaType(post.getMediaType())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getSharesCount())
                .viewsCount(post.getViewsCount())
                .userLiked(userLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .recentComments(recentComments)
                .build();
    }

    private String getUserRole(org.springframework.security.oauth2.jwt.Jwt jwt) {
        return RoleUtils.getUserRole(jwt);
    }

    private boolean isAdmin(String role) {
        return RoleUtils.isAdmin(role);
    }
}