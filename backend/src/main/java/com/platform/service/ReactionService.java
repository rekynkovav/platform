// src/main/java/com/platform/service/ReactionService.java
package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Post;
import com.platform.model.Reaction;
import com.platform.model.dto.ReactionDto;
import com.platform.repository.PostRepository;
import com.platform.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;

    @Transactional
    public ReactionDto toggleReaction(String postId, String type, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String userId = jwt.getSubject();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        Optional<Reaction> existingReaction = reactionRepository
                .findByPostIdAndUserIdAndType(postId, userId, type);

        if (existingReaction.isPresent()) {
            reactionRepository.delete(existingReaction.get());
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            postRepository.save(post);
            log.info("Reaction removed: {} on post {} by {}", type, postId, userId);

            return ReactionDto.builder()
                    .postId(postId)
                    .userId(userId)
                    .type(type)
                    .count((long) post.getLikesCount())
                    .userReacted(false)
                    .build();
        } else {
            Reaction reaction = Reaction.builder()
                    .id(UUID.randomUUID().toString())
                    .postId(postId)
                    .userId(userId)
                    .type(type)
                    .createdAt(LocalDateTime.now())
                    .build();

            reactionRepository.save(reaction);
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);
            log.info("Reaction added: {} on post {} by {}", type, postId, userId);

            return ReactionDto.builder()
                    .postId(postId)
                    .userId(userId)
                    .type(type)
                    .count((long) post.getLikesCount())
                    .userReacted(true)
                    .build();
        }
    }

    public ReactionDto getReactionStatus(String postId, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String userId = jwt.getSubject();

        long count = reactionRepository.countByPostIdAndType(postId, "like");
        boolean userReacted = reactionRepository.existsByPostIdAndUserIdAndType(postId, userId, "like");

        return ReactionDto.builder()
                .postId(postId)
                .userId(userId)
                .type("like")
                .count(count)
                .userReacted(userReacted)
                .build();
    }
}