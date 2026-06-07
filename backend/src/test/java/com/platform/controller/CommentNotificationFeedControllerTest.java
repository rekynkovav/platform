package com.platform.controller;

import com.platform.model.Notification;
import com.platform.model.dto.CommentDto;
import com.platform.model.dto.PostDto;
import com.platform.service.CommentService;
import com.platform.service.NotificationService;
import com.platform.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentNotificationFeedControllerTest {

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .build();
    }

    // ==================== CommentController ====================

    @Test
    void commentGetComments_ReturnsListFromService() {
        CommentService commentService = mock(CommentService.class);
        when(commentService.getCommentsByPost("post-1", 50)).thenReturn(List.of(new CommentDto()));

        CommentController controller = new CommentController(commentService);
        ResponseEntity<List<CommentDto>> response = controller.getComments("post-1", 50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void commentCreate_ReturnsCreatedComment() {
        CommentService commentService = mock(CommentService.class);
        CommentDto expected = new CommentDto();
        CommentDto.CreateRequest request = new CommentDto.CreateRequest();
        when(commentService.createComment(eq("post-1"), any(), eq(jwt))).thenReturn(expected);

        CommentController controller = new CommentController(commentService);
        ResponseEntity<CommentDto> response = controller.createComment("post-1", request, jwt);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void commentDelete_ReturnsNoContent() {
        CommentService commentService = mock(CommentService.class);

        CommentController controller = new CommentController(commentService);
        ResponseEntity<Void> response = controller.deleteComment("post-1", "comment-1", jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(commentService).deleteComment("post-1", "comment-1", jwt);
    }

    // ==================== NotificationController ====================

    @Test
    void notificationGetAll_ReturnsListFromService() {
        NotificationService notificationService = mock(NotificationService.class);
        when(notificationService.getAllForUser("user-123")).thenReturn(List.of(new Notification()));

        NotificationController controller = new NotificationController(notificationService);
        ResponseEntity<List<Notification>> response = controller.getAll(jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void notificationGetUnread_ReturnsListFromService() {
        NotificationService notificationService = mock(NotificationService.class);
        when(notificationService.getUnreadForUser("user-123")).thenReturn(List.of());

        NotificationController controller = new NotificationController(notificationService);
        ResponseEntity<List<Notification>> response = controller.getUnread(jwt);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void notificationMarkAsRead_ReturnsNoContent() {
        NotificationService notificationService = mock(NotificationService.class);

        NotificationController controller = new NotificationController(notificationService);
        ResponseEntity<Void> response = controller.markAsRead(1L, jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(notificationService).markAsReadForUser(1L, "user-123");
    }

    @Test
    void notificationMarkAllAsRead_ReturnsNoContent() {
        NotificationService notificationService = mock(NotificationService.class);

        NotificationController controller = new NotificationController(notificationService);
        ResponseEntity<Void> response = controller.markAllAsRead(jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(notificationService).markAllAsRead("user-123");
    }

    @Test
    void notificationSync_UpdatesReadNotifications() {
        NotificationService notificationService = mock(NotificationService.class);
        List<Map<String, Object>> notifications = List.of(
                Map.of("id", 1L, "read", true),
                Map.of("id", 2L, "read", false),
                Map.of("id", 3L, "read", true)
        );

        NotificationController controller = new NotificationController(notificationService);
        ResponseEntity<Map<String, Object>> response = controller.syncNotifications(notifications, jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().get("synced"));
        assertEquals(3, response.getBody().get("total"));
        verify(notificationService, times(2)).markAsReadForUser(anyLong(), eq("user-123"));
    }

    @Test
    void notificationSync_SkipsNonNumericIds() {
        NotificationService notificationService = mock(NotificationService.class);
        List<Map<String, Object>> notifications = List.of(
                Map.of("id", "local-uuid", "read", true)
        );

        NotificationController controller = new NotificationController(notificationService);
        ResponseEntity<Map<String, Object>> response = controller.syncNotifications(notifications, jwt);

        assertEquals(0, response.getBody().get("synced"));
        verifyNoInteractions(notificationService);
    }

    // ==================== FeedController ====================

    @Test
    void feedGetMain_ReturnsListFromService() {
        PostService postService = mock(PostService.class);
        when(postService.getFeed(eq("user-123"), isNull(), any(Pageable.class)))
                .thenReturn(List.of(new PostDto()));

        FeedController controller = new FeedController(postService);
        ResponseEntity<List<PostDto>> response = controller.getMainFeed(50, null, jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void feedGetNutrition_ReturnsListFromService() {
        PostService postService = mock(PostService.class);
        when(postService.getFeedByCategory(eq("nutrition"), eq("user-123"), isNull(), any(Pageable.class)))
                .thenReturn(List.of());

        FeedController controller = new FeedController(postService);
        ResponseEntity<List<PostDto>> response = controller.getNutritionFeed(50, null, jwt);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void feedDeletePost_ReturnsNoContent() {
        PostService postService = mock(PostService.class);

        FeedController controller = new FeedController(postService);
        ResponseEntity<Void> response = controller.deletePost("post-1", jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(postService).deletePost("post-1", jwt);
    }
}
