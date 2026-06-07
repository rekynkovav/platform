package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Post;
import com.platform.model.User;
import com.platform.model.dto.AdminStatsDto;
import com.platform.model.dto.UserDto;
import com.platform.repository.PostRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentService commentService;
    @Mock private EventService eventService;

    @InjectMocks
    private AdminService adminService;

    private UUID userId;
    private User user;
    private Jwt superAdminJwt;
    private Jwt moderatorJwt;
    private Jwt regularUserJwt;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .role("USER_0")
                .banned(false)
                .createdAt(LocalDateTime.now())
                .build();

        // RoleUtils expects realm_access.roles as flat string claim
        superAdminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("realm_access.roles", "SUPER_ADMIN")
                .build();

        moderatorJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("realm_access.roles", "MODERATOR_ADMIN")
                .build();

        regularUserJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("realm_access.roles", "USER_0")
                .build();
    }

    // ==================== getAllUsers ====================

    @Test
    void getAllUsers_ReturnsPageOfUserDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(usersPage);

        Page<UserDto> result = adminService.getAllUsers(null, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    void getAllUsers_EmptyPage_ReturnsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<UserDto> result = adminService.getAllUsers(null, pageable);

        assertThat(result).isEmpty();
    }

    // ==================== getUserById ====================

    @Test
    void getUserById_Exists_ReturnsUserDto() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto result = adminService.getUserById(userId.toString());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId.toString());
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(userId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== updateUserRole ====================

    @Test
    void updateUserRole_SuperAdmin_UpdatesRole() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = adminService.updateUserRole(userId.toString(), "MODERATOR_ADMIN", superAdminJwt);

        assertThat(result.getRole()).isEqualTo("MODERATOR_ADMIN");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserRole_RegularUser_ThrowsAccessDenied() {
        assertThatThrownBy(() -> adminService.updateUserRole(userId.toString(), "ADMIN", regularUserJwt))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserRole(userId.toString(), "ADMIN", superAdminJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== banUser ====================

    @Test
    void banUser_ModeratorAdmin_BansUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.banUser(userId.toString(), "Spam", moderatorJwt);

        assertThat(user.isBanned()).isTrue();
        assertThat(user.getBanReason()).isEqualTo("Spam");
        assertThat(user.getBannedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void banUser_SuperAdmin_BansUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.banUser(userId.toString(), "Violation", superAdminJwt);

        assertThat(user.isBanned()).isTrue();
    }

    @Test
    void banUser_RegularUser_ThrowsAccessDenied() {
        assertThatThrownBy(() -> adminService.banUser(userId.toString(), "reason", regularUserJwt))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void banUser_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.banUser(userId.toString(), "reason", superAdminJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== unbanUser ====================

    @Test
    void unbanUser_ModeratorAdmin_UnbansUser() {
        user.setBanned(true);
        user.setBanReason("Spam");
        user.setBannedAt(LocalDateTime.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.unbanUser(userId.toString(), moderatorJwt);

        assertThat(user.isBanned()).isFalse();
        assertThat(user.getBanReason()).isNull();
        assertThat(user.getBannedAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void unbanUser_RegularUser_ThrowsAccessDenied() {
        assertThatThrownBy(() -> adminService.unbanUser(userId.toString(), regularUserJwt))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void unbanUser_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.unbanUser(userId.toString(), superAdminJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== getFlaggedPosts ====================

    @Test
    void getFlaggedPosts_ReturnsList() {
        Post flagged = Post.builder().id("post-1").content("Flagged content").flagged(true).build();
        when(postRepository.findByIsFlaggedTrue()).thenReturn(List.of(flagged));

        List<Post> result = adminService.getFlaggedPosts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isFlagged()).isTrue();
    }

    @Test
    void getFlaggedPosts_NoFlagged_ReturnsEmpty() {
        when(postRepository.findByIsFlaggedTrue()).thenReturn(List.of());

        List<Post> result = adminService.getFlaggedPosts();

        assertThat(result).isEmpty();
    }

    // ==================== moderatePost ====================

    @Test
    void moderatePost_Approve_UnflagsAndSetsModerated() {
        Post post = Post.builder().id("post-1").content("Test content").flagged(true).build();
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.moderatePost("post-1", true, null, superAdminJwt);

        assertThat(post.isFlagged()).isFalse();
        assertThat(post.getModeratedAt()).isNotNull();
        assertThat(post.getModeratedBy()).isNotNull();
        verify(postRepository).save(post);
    }

    @Test
    void moderatePost_Reject_SoftDeletesAndSetsReason() {
        Post post = Post.builder().id("post-1").content("Bad post content").flagged(true).build();
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.moderatePost("post-1", false, "Inappropriate content", superAdminJwt);

        assertThat(post.getDeletedAt()).isNotNull();
        assertThat(post.getModerationReason()).isEqualTo("Inappropriate content");
        assertThat(post.getModeratedAt()).isNotNull();
    }

    @Test
    void moderatePost_NotFound_ThrowsException() {
        when(postRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.moderatePost("nonexistent", true, null, superAdminJwt))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== getStats ====================

    @Test
    void getStats_ReturnsAggregatedStats() {
        when(userRepository.count()).thenReturn(100L);
        when(postRepository.count()).thenReturn(500L);
        when(commentService.getTotalCount()).thenReturn(2000L);
        when(eventService.getTotalCount()).thenReturn(50L);
        when(userRepository.countByLastActiveAtAfter(any(LocalDateTime.class))).thenReturn(30L);

        AdminStatsDto stats = adminService.getStats();

        assertThat(stats.getTotalUsers()).isEqualTo(100L);
        assertThat(stats.getTotalPosts()).isEqualTo(500L);
        assertThat(stats.getTotalComments()).isEqualTo(2000L);
        assertThat(stats.getTotalEvents()).isEqualTo(50L);
        assertThat(stats.getActiveUsers()).isEqualTo(30L);
    }
}
