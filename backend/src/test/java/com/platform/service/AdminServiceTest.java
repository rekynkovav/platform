package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Post;
import com.platform.model.User;
import com.platform.model.dto.AdminStatsDto;
import com.platform.model.dto.UserDto;
import com.platform.repository.UserRepository;
import com.platform.repository.PostRepository;
import com.platform.repository.CommentRepository;
import com.platform.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CommentService commentService;

    @Mock
    private EventService eventService;

    @InjectMocks
    private AdminService adminService;

    private String adminUserId;
    private UUID adminUserIdUuid;
    private String targetUserId;
    private UUID targetUserIdUuid;
    private String regularUserId;
    private String postId;

    @BeforeEach
    void setUp() {
        adminUserId = "admin-123";
        adminUserIdUuid = UUID.fromString(adminUserId);
        targetUserId = "target-456";
        targetUserIdUuid = UUID.fromString(targetUserId);
        regularUserId = "user-789";
        postId = "post-999";
    }

    @Test
    void updateUserRole_BySuperAdmin_UpdatesRole() {
        // Arrange
        String newRole = "MODERATOR_ADMIN";
        User targetUser = User.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .role("USER_1")
                .build();

        when(userRepository.findById(targetUserIdUuid)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        // Mock super admin check
        when(adminService.isSuperAdmin(adminUserId)).thenReturn(true);

        // Act
        adminService.updateUserRole(targetUserId, newRole, adminUserId);

        // Assert
        assertThat(targetUser.getRole()).isEqualTo(newRole);
        verify(userRepository).save(targetUser);
    }

    @Test
    void updateUserRole_ByVyacheslav_UpdatesRole() {
        // Arrange
        String newRole = "MODERATOR_ADMIN";
        String vyacheslavUserId = "vyacheslav-001";
        User targetUser = User.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .role("USER_1")
                .build();

        when(userRepository.findById(targetUserIdUuid)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        // Mock VYACHESLAV check
        when(adminService.isVyacheslav(vyacheslavUserId)).thenReturn(true);

        // Act
        adminService.updateUserRole(targetUserId, newRole, vyacheslavUserId);

        // Assert
        assertThat(targetUser.getRole()).isEqualTo(newRole);
        verify(userRepository).save(targetUser);
    }

    @Test
    void updateUserRole_ByModeratorAdmin_ThrowsException() {
        // Arrange
        String newRole = "MODERATOR_ADMIN";
        String moderatorUserId = "moderator-555";
        User targetUser = User.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .role("USER_1")
                .build();

        when(userRepository.findById(targetUserIdUuid)).thenReturn(Optional.of(targetUser));
        when(adminService.isSuperAdmin(moderatorUserId)).thenReturn(false);
        when(adminService.isVyacheslav(moderatorUserId)).thenReturn(false);
        when(adminService.isModeratorAdmin(moderatorUserId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> adminService.updateUserRole(targetUserId, newRole, moderatorUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRole_ByRegularUser_ThrowsException() {
        // Arrange
        String newRole = "MODERATOR_ADMIN";
        User targetUser = User.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .role("USER_1")
                .build();

        when(userRepository.findById(targetUserIdUuid)).thenReturn(Optional.of(targetUser));
        when(adminService.isSuperAdmin(regularUserId)).thenReturn(false);
        when(adminService.isVyacheslav(regularUserId)).thenReturn(false);
        when(adminService.isModeratorAdmin(regularUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adminService.updateUserRole(targetUserId, newRole, regularUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void banUser_ByModerator_BansUser() {
        // Arrange
        String banReason = "Violation of community guidelines";
        User targetUser = User.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .banned(false)
                .banReason(null)
                .bannedAt(null)
                .build();

        when(userRepository.findById(targetUserIdUuid)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        // Mock moderator check
        when(adminService.isModerator(adminUserId)).thenReturn(true);

        // Act
        adminService.banUser(targetUserId, banReason, adminUserId);

        // Assert
        assertThat(targetUser.isBanned()).isTrue();
        assertThat(targetUser.getBanReason()).isEqualTo(banReason);
        assertThat(targetUser.getBannedAt()).isNotNull();
        verify(userRepository).save(targetUser);
    }

    @Test
    void banUser_ByRegularUser_ThrowsException() {
        // Arrange
        String banReason = "Some reason";
        User targetUser = User.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .banned(false)
                .build();

        when(userRepository.findById(targetUserIdUuid)).thenReturn(Optional.of(targetUser));
        when(adminService.isModerator(regularUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adminService.banUser(targetUserId, banReason, regularUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void unbanUser_ByAdmin_UnbansUser() {
        // Arrange
        User targetUser = User.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .banned(true)
                .banReason("Old reason")
                .bannedAt(Instant.now().minusDays(5))
                .build();

        when(userRepository.findById(targetUserIdUuid)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        // Mock admin check
        when(adminService.isAdmin(adminUserId)).thenReturn(true);

        // Act
        adminService.unbanUser(targetUserId, adminUserId);

        // Assert
        assertThat(targetUser.isBanned()).isFalse();
        assertThat(targetUser.getBanReason()).isNull();
        verify(userRepository).save(targetUser);
    }

    @Test
    void moderatePost_Approve_UnflagsPost() {
        // Arrange
        String moderationReason = "Compliant with guidelines";
        Post post = Post.builder()
                .id(postId)
                .authorId(targetUserIdUuid)
                .content("Test post")
                .flagged(true)
                .moderatedAt(null)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // Act
        adminService.moderatePost(postId, true, moderationReason, adminUserId);

        // Assert
        assertThat(post.isFlagged()).isFalse();
        assertThat(post.getModeratedAt()).isNotNull();
        verify(postRepository).save(post);
    }

    @Test
    void moderatePost_Reject_SoftDeletesPost() {
        // Arrange
        String moderationReason = "Violates community guidelines";
        Post post = Post.builder()
                .id(postId)
                .authorId(targetUserIdUuid)
                .content("Test post")
                .flagged(true)
                .deletedAt(null)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // Act
        adminService.moderatePost(postId, false, moderationReason, adminUserId);

        // Assert
        assertThat(post.getDeletedAt()).isNotNull();
        assertThat(post.getModerationReason()).isEqualTo(moderationReason);
        verify(postRepository).save(post);
    }

    @Test
    void getStats_ReturnsAllStatistics() {
        // Arrange
        long totalUsers = 150L;
        long totalPosts = 300L;
        long totalComments = 450L;
        long totalEvents = 60L;
        long activeUsers = 75L;  // Last 7 days

        when(userRepository.count()).thenReturn(totalUsers);
        when(postRepository.count()).thenReturn(totalPosts);
        when(commentRepository.count()).thenReturn(totalComments);
        when(eventRepository.count()).thenReturn(totalEvents);
        when(userRepository.countByLastLoginAfter(any(Instant.class))).thenReturn(activeUsers);

        // Act
        AdminStatsDto result = adminService.getStats();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalUsers()).isEqualTo(totalUsers);
        assertThat(result.getTotalPosts()).isEqualTo(totalPosts);
        assertThat(result.getTotalComments()).isEqualTo(totalComments);
        assertThat(result.getTotalEvents()).isEqualTo(totalEvents);
        assertThat(result.getActiveUsers()).isEqualTo(activeUsers);
    }

    @Test
    void getUserById_Found_ReturnsUserDto() {
        // Arrange
        User user = User.builder()
                .id(targetUserIdUuid)
                .username("targetuser")
                .email("target@example.com")
                .role("USER_1")
                .banned(false)
                .createdAt(Instant.now().minusDays(30))
                .lastLogin(Instant.now().minusHours(2))
                .build();

        when(userRepository.findById(targetUserIdUuid)).thenReturn(Optional.of(user));

        // Act
        UserDto result = adminService.getUserById(targetUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(targetUserIdUuid);
        assertThat(result.getUsername()).isEqualTo("targetuser");
        assertThat(result.getEmail()).isEqualTo("target@example.com");
        assertThat(result.getRole()).isEqualTo("USER_1");
        assertThat(result.isBanned()).isFalse();
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        // Arrange
        String nonExistentUserId = "non-existent-user";
        UUID nonExistentUuid = UUID.fromString(nonExistentUserId);
        when(userRepository.findById(nonExistentUuid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.getUserById(nonExistentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}