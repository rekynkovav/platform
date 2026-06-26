package com.platform.service;

import com.platform.util.RoleUtils;
import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Comment;
import com.platform.model.Post;
import com.platform.model.dto.CommentDto;
import com.platform.repository.CommentRepository;
import com.platform.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public List<CommentDto> getCommentsByPost(String postId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);

        return comments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CommentDto> getRecentComments(String postId, int limit) {
        return getCommentsByPost(postId, limit);
    }

    /**
     * Batch fetch top-N comments per post IDs.
     * Returns Map&lt;postId, List&lt;CommentDto&gt;&gt; — eliminates N+1 queries.
     */
    public Map<String, List<CommentDto>> getRecentCommentsByPostIds(Set<String> postIds, int topN) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        List<Comment> allComments = commentRepository.findTopNByPostIds(postIds, topN);
        Map<String, List<CommentDto>> result = new LinkedHashMap<>();
        for (String postId : postIds) {
            result.put(postId, new ArrayList<>());
        }
        for (Comment comment : allComments) {
            result.computeIfAbsent(comment.getPostId(), k -> new ArrayList<>())
                  .add(toDto(comment));
        }
        return result;
    }

    public long getTotalCount() {
        return commentRepository.count();
    }

    @Transactional
    public CommentDto createComment(String postId, CommentDto.CreateRequest request,
                                    org.springframework.security.oauth2.jwt.Jwt jwt) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        String userId = jwt.getSubject();
        String userName = jwt.getClaim("preferred_username");

        Comment comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .content(request.getContent())
                .postId(postId)
                .authorId(userId)
                .authorName(userName)
                .createdAt(LocalDateTime.now())
                .build();

        comment = commentRepository.save(comment);

        // Обновляем счётчик комментариев у поста
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        log.info("Comment created: {} on post {} by {}", comment.getId(), postId, userId);

        return toDto(comment);
    }

    @Transactional
    public void deleteComment(String postId, String commentId, org.springframework.security.oauth2.jwt.Jwt jwt) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
        // Проверяем, что комментарий принадлежит этому посту
        if (!comment.getPostId().equals(postId)) {
            throw new IllegalArgumentException("Comment does not belong to this post");
        }

        String userId = jwt.getSubject();
        String userRole = RoleUtils.getUserRole(jwt);

        if (!comment.getAuthorId().equals(userId) && !RoleUtils.isAdmin(userRole)) {
            throw new AccessDeniedException("You don't have permission to delete this comment");
        }

        commentRepository.delete(comment);

        // Обновляем счётчик комментариев у поста
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null && post.getCommentsCount() > 0) {
            post.setCommentsCount(post.getCommentsCount() - 1);
            postRepository.save(post);
        }

        log.info("Comment deleted: {} by {}", commentId, userId);
    }

    private CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .authorName(comment.getAuthorName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
