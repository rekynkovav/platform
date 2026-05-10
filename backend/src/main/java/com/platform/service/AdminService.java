package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Post;
import com.platform.model.User;
import com.platform.model.dto.AdminStatsDto;
import com.platform.model.dto.UserDto;
import com.platform.repository.PostRepository;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentService commentService;
    private final EventService eventService;

    public List<UserDto> getAllUsers(String search, int limit, int offset) {
        List<User> users;
        if (search != null && !search.isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCase(search);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .skip(offset)
                .limit(limit)
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    public UserDto getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return toUserDto(user);
    }

    @Transactional
    public UserDto updateUserRole(String userId, String role, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String currentUserId = jwt.getSubject();
        String currentUserRole = getUserRole(jwt);

        // Только супер-админ может менять роли
        if (!"SUPER_ADMIN".equals(currentUserRole) && !"VYACHESLAV".equals(currentUserRole)) {
            throw new AccessDeniedException("Only super admin can change user roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        log.info("User role updated: {} -> {} by {}", userId, role, currentUserId);
        return toUserDto(user);
    }

    @Transactional
    public void banUser(String userId, String reason, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String currentUserId = jwt.getSubject();
        String currentUserRole = getUserRole(jwt);

        if (!"MODERATOR_ADMIN".equals(currentUserRole) && !"SUPER_ADMIN".equals(currentUserRole) && !"VYACHESLAV".equals(currentUserRole)) {
            throw new AccessDeniedException("Only admin can ban users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setBanned(true);
        user.setBanReason(reason);
        user.setBannedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User banned: {} by {}, reason: {}", userId, currentUserId, reason);
    }

    @Transactional
    public void unbanUser(String userId, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String currentUserId = jwt.getSubject();
        String currentUserRole = getUserRole(jwt);

        if (!"MODERATOR_ADMIN".equals(currentUserRole) && !"SUPER_ADMIN".equals(currentUserRole) && !"VYACHESLAV".equals(currentUserRole)) {
            throw new AccessDeniedException("Only admin can unban users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setBanned(false);
        user.setBanReason(null);
        user.setBannedAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User unbanned: {} by {}", userId, currentUserId);
    }

    public List<Post> getFlaggedPosts() {
        return postRepository.findByIsFlaggedTrue();
    }

    @Transactional
    public void moderatePost(String postId, boolean approve, String reason, org.springframework.security.oauth2.jwt.Jwt jwt) {
        String currentUserId = jwt.getSubject();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        if (approve) {
            post.setFlagged(false);
            post.setModeratedAt(LocalDateTime.now());
            post.setModeratedBy(currentUserId);
            log.info("Post approved: {} by {}", postId, currentUserId);
        } else {
            // Soft delete
            post.setDeletedAt(LocalDateTime.now());
            post.setModeratedAt(LocalDateTime.now());
            post.setModeratedBy(currentUserId);
            post.setModerationReason(reason);
            log.info("Post rejected and deleted: {} by {}, reason: {}", postId, currentUserId, reason);
        }

        postRepository.save(post);
    }

    public AdminStatsDto getStats() {
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long totalComments = commentService.getTotalCount();
        long totalEvents = eventService.getTotalCount();
        long activeUsers = userRepository.countByLastActiveAtAfter(LocalDateTime.now().minusDays(7));

        return AdminStatsDto.builder()
                .totalUsers(totalUsers)
                .totalPosts(totalPosts)
                .totalComments(totalComments)
                .totalEvents(totalEvents)
                .activeUsers(activeUsers)
                .build();
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .banned(user.isBanned())
                .banReason(user.getBanReason())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String getUserRole(org.springframework.security.oauth2.jwt.Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles == null) return "user";
        if (roles.contains("SUPER_ADMIN")) return "SUPER_ADMIN";
        if (roles.contains("MODERATOR_ADMIN")) return "MODERATOR_ADMIN";
        if (roles.contains("VYACHESLAV")) return "VYACHESLAV";
        return "user";
    }
}
