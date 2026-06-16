package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.ChatRoom;
import com.platform.model.MediaFile;
import com.platform.model.Message;
import com.platform.model.dto.MessageDto;
import com.platform.model.dto.QuestionDto;
import com.platform.repository.ChatRoomPermissionRepository;
import com.platform.repository.ChatRoomRepository;
import com.platform.repository.MediaFileRepository;
import com.platform.repository.MessageRepository;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserService userService;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomService chatRoomService;
    @Mock private ChatRoomPermissionRepository chatRoomPermissionRepository;

    @InjectMocks
    private ChatService chatService;

    private String userId;
    private String otherUserId;
    private UUID userUuid;
    private UUID roomUuid;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        otherUserId = UUID.randomUUID().toString();
        userUuid = UUID.fromString(userId);
        roomUuid = UUID.randomUUID();

        chatRoom = ChatRoom.builder()
                .id(roomUuid)
                .slug("general")
                .name("General Chat")
                .description("General discussion")
                .build();
    }

    // ==================== getGeneralMessages ====================

    @Test
    void getGeneralMessages_NoBefore_ReturnsMessages() {
        Message msg = buildMessage("general", "Hello");
        when(messageRepository.findByChatTypeOrderByCreatedAtDesc(eq("general"), any(Pageable.class)))
                .thenReturn(List.of(msg));

        List<MessageDto> result = chatService.getGeneralMessages(20, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Hello");
    }

    @Test
    void getGeneralMessages_WithBefore_ReturnsMessagesBeforeDate() {
        String before = Instant.now().toString();
        Message msg = buildMessage("general", "Old message");
        when(messageRepository.findByChatTypeAndCreatedAtBefore(eq("general"), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(msg));

        List<MessageDto> result = chatService.getGeneralMessages(20, before);

        assertThat(result).hasSize(1);
    }

    @Test
    void getGeneralMessages_InvalidDateFormat_ThrowsException() {
        assertThatThrownBy(() -> chatService.getGeneralMessages(20, "not-a-date"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== getAdminGroupMessages ====================

    @Test
    void getAdminGroupMessages_NoBefore_ReturnsMessages() {
        Message msg = buildMessage("admin_group", "Admin msg");
        when(messageRepository.findByChatTypeOrderByCreatedAtDesc(eq("admin_group"), any(Pageable.class)))
                .thenReturn(List.of(msg));

        List<MessageDto> result = chatService.getAdminGroupMessages(10, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChatType()).isEqualTo("admin_group");
    }

    @Test
    void getAdminGroupMessages_WithBefore_ReturnsFiltered() {
        String before = Instant.now().toString();
        when(messageRepository.findByChatTypeAndCreatedAtBefore(eq("admin_group"), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        List<MessageDto> result = chatService.getAdminGroupMessages(10, before);

        assertThat(result).isEmpty();
    }

    // ==================== getPrivateMessages ====================

    @Test
    void getPrivateMessages_NoBefore_ReturnsMessages() {
        Message msg = buildMessage("private", "Private hello");
        when(messageRepository.findPrivateMessages(eq(UUID.fromString(userId)), eq(UUID.fromString(otherUserId)), any(Pageable.class)))
                .thenReturn(List.of(msg));

        List<MessageDto> result = chatService.getPrivateMessages(userId, otherUserId, 20, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPrivateMessages_WithBefore_ReturnsFiltered() {
        String before = Instant.now().toString();
        when(messageRepository.findPrivateMessagesBefore(eq(UUID.fromString(userId)), eq(UUID.fromString(otherUserId)), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        List<MessageDto> result = chatService.getPrivateMessages(userId, otherUserId, 20, before);

        assertThat(result).isEmpty();
    }

    // ==================== sendToGeneralChat ====================

    @Test
    void sendToGeneralChat_WithoutMedia_SavesAndReturns() {
        when(userService.getUserName(userId)).thenReturn("TestUser");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageDto result = chatService.sendToGeneralChat(userId, "Hello general");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Hello general");
        assertThat(result.getChatType()).isEqualTo("general");
        assertThat(result.getUserName()).isEqualTo("TestUser");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendToGeneralChat_WithMedia_IncludesMediaInfo() {
        String mediaId = "media-123";
        MediaFile media = new MediaFile();
        media.setFileType("image/png");
        media.setOriginalFilename("photo.png");

        when(userService.getUserName(userId)).thenReturn("TestUser");
        when(mediaFileRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageDto result = chatService.sendToGeneralChat(userId, "With photo", mediaId);

        assertThat(result.getMediaId()).isEqualTo(mediaId);
        assertThat(result.getMediaType()).isEqualTo("image/png");
        assertThat(result.getFileName()).isEqualTo("photo.png");
    }

    // ==================== sendToAdminGroup ====================

    @Test
    void sendToAdminGroup_SavesAndReturns() {
        when(userService.getUserName(userId)).thenReturn("Admin");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageDto result = chatService.sendToAdminGroup(userId, "Admin announcement");

        assertThat(result.getChatType()).isEqualTo("admin_group");
        assertThat(result.getContent()).isEqualTo("Admin announcement");
    }

    // ==================== sendPrivateMessage ====================

    @Test
    void sendPrivateMessage_SavesWithRecipient() {
        when(userService.getUserName(userId)).thenReturn("Sender");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageDto result = chatService.sendPrivateMessage(userId, otherUserId, "Secret msg");

        assertThat(result.getChatType()).isEqualTo("private");
        assertThat(result.getRecipientId()).isEqualTo(UUID.fromString(otherUserId));
        assertThat(result.getContent()).isEqualTo("Secret msg");
    }

    // ==================== markAsRead ====================

    @Test
    void markAsRead_RecipientMatches_MarksRead() {
        UUID msgId = UUID.randomUUID();
        Message msg = Message.builder()
                .id(msgId).recipientId(userUuid).isRead(false).build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        chatService.markAsRead(msgId.toString(), userId);

        assertThat(msg.getIsRead()).isTrue();
        verify(messageRepository).save(msg);
    }

    @Test
    void markAsRead_RecipientDoesNotMatch_DoesNotMark() {
        UUID msgId = UUID.randomUUID();
        UUID otherRecipient = UUID.randomUUID();
        Message msg = Message.builder()
                .id(msgId).recipientId(otherRecipient).isRead(false).build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

        chatService.markAsRead(msgId.toString(), userId);

        assertThat(msg.getIsRead()).isFalse();
        verify(messageRepository, never()).save(any());
    }

    @Test
    void markAsRead_MessageNotFound_ThrowsException() {
        UUID msgId = UUID.randomUUID();
        when(messageRepository.findById(msgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.markAsRead(msgId.toString(), userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== getUnreadCount ====================

    @Test
    void getUnreadCount_ReturnsCount() {
        when(messageRepository.countUnreadByRecipient(UUID.fromString(userId))).thenReturn(5L);

        long count = chatService.getUnreadCount(userId);

        assertThat(count).isEqualTo(5L);
    }

    // ==================== getMessages (paginated) ====================

    @Test
    void getMessages_ReturnsPage() {
        Page<MessageDto> page = new PageImpl<>(List.of());
        when(messageRepository.findMessageDtosByRoomId(eq(roomUuid), any(Pageable.class)))
                .thenReturn(page);

        Page<MessageDto> result = chatService.getMessages(roomUuid.toString(), 0, 20);

        assertThat(result).isNotNull();
    }

    // ==================== getMessagesAfter (cursor) ====================

    @Test
    void getMessagesAfter_WithCursor_ReturnsMessagesBeforeCursor() {
        UUID cursorId = UUID.randomUUID();
        Message msg = buildMessage("room", "After cursor");
        when(messageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(eq(roomUuid), eq(cursorId), any(Pageable.class)))
                .thenReturn(List.of(msg));

        List<MessageDto> result = chatService.getMessagesAfter(roomUuid.toString(), cursorId.toString(), 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void getMessagesAfter_NullCursor_ReturnsLatestMessages() {
        Message msg = buildMessage("room", "Latest");
        when(messageRepository.findByRoomId(eq(roomUuid), any(Pageable.class)))
                .thenReturn(List.of(msg));

        List<MessageDto> result = chatService.getMessagesAfter(roomUuid.toString(), null, 10);

        assertThat(result).hasSize(1);
    }

    // ==================== getMessagesByRoom ====================

    @Test
    void getMessagesByRoom_HasPermission_ReturnsMessages() {
        when(chatRoomRepository.findBySlug("general")).thenReturn(Optional.of(chatRoom));
        when(chatRoomService.hasPermission(roomUuid, "USER", "read")).thenReturn(true);
        Message msg = buildMessage("room", "Room msg");
        when(messageRepository.findByRoomIdOrderByCreatedAtDesc(eq(roomUuid), any(Pageable.class)))
                .thenReturn(List.of(msg));

        List<MessageDto> result = chatService.getMessagesByRoom("general", 20, null, "USER");

        assertThat(result).hasSize(1);
    }

    @Test
    void getMessagesByRoom_NoPermission_ThrowsAccessDenied() {
        when(chatRoomRepository.findBySlug("general")).thenReturn(Optional.of(chatRoom));
        when(chatRoomService.hasPermission(roomUuid, "GUEST", "read")).thenReturn(false);

        assertThatThrownBy(() -> chatService.getMessagesByRoom("general", 20, null, "GUEST"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMessagesByRoom_RoomNotFound_ThrowsException() {
        when(chatRoomRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessagesByRoom("nonexistent", 20, null, "USER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== sendMessageToRoom ====================

    @Test
    void sendMessageToRoom_HasPermission_SavesAndReturns() {
        when(chatRoomRepository.findBySlug("general")).thenReturn(Optional.of(chatRoom));
        when(chatRoomService.hasPermission(roomUuid, "USER", "read_write")).thenReturn(true);
        when(userService.getUserName(userId)).thenReturn("TestUser");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageDto result = chatService.sendMessageToRoom("general", userId, "Hello room", null, "USER");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Hello room");
        assertThat(result.getRoomSlug()).isEqualTo("general");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessageToRoom_NoPermission_ThrowsAccessDenied() {
        when(chatRoomRepository.findBySlug("general")).thenReturn(Optional.of(chatRoom));
        when(chatRoomService.hasPermission(roomUuid, "GUEST", "read_write")).thenReturn(false);

        assertThatThrownBy(() -> chatService.sendMessageToRoom("general", userId, "Hi", null, "GUEST"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ==================== createQuestion ====================

    @Test
    void createQuestion_CreatesAndReturns() {
        ChatRoom questionRoom = ChatRoom.builder()
                .id(roomUuid).slug("questions-nutrition").name("Questions").build();
        when(chatRoomRepository.findBySlug("questions-nutrition")).thenReturn(Optional.of(questionRoom));
        when(userService.getUserName(userId)).thenReturn("Asker");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            return m;
        });

        QuestionDto.CreateRequest request = new QuestionDto.CreateRequest();
        request.setQuestion("How to eat healthy?");

        QuestionDto result = chatService.createQuestion("nutrition", request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getQuestion()).isEqualTo("How to eat healthy?");
        assertThat(result.getMode()).isEqualTo("nutrition");
        assertThat(result.isAnswered()).isFalse();
    }

    @Test
    void createQuestion_RoomNotFound_ThrowsException() {
        when(chatRoomRepository.findBySlug("questions-fake")).thenReturn(Optional.empty());

        QuestionDto.CreateRequest request = new QuestionDto.CreateRequest();
        request.setQuestion("Test?");

        assertThatThrownBy(() -> chatService.createQuestion("fake", request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== answerQuestion ====================

    @Test
    void answerQuestion_AnswersAndReturns() {
        UUID questionId = UUID.randomUUID();
        Message questionMsg = Message.builder()
                .id(questionId).userId(userUuid).userName("Asker")
                .content("What is protein?").roomId(roomUuid).roomSlug("questions-nutrition")
                .chatType("question").createdAt(LocalDateTime.now()).build();

        when(messageRepository.findById(questionId)).thenReturn(Optional.of(questionMsg));
        when(userService.getUserName(userId)).thenReturn("AdminDoc");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionDto.AnswerRequest request = new QuestionDto.AnswerRequest();
        request.setAnswer("Protein is essential nutrient.");

        QuestionDto result = chatService.answerQuestion(questionId.toString(), request, userId);

        assertThat(result.isAnswered()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("Protein is essential nutrient.");
        assertThat(result.getAnsweredBy()).isEqualTo("AdminDoc");
    }

    @Test
    void answerQuestion_QuestionNotFound_ThrowsException() {
        UUID questionId = UUID.randomUUID();
        when(messageRepository.findById(questionId)).thenReturn(Optional.empty());

        QuestionDto.AnswerRequest request = new QuestionDto.AnswerRequest();
        request.setAnswer("Answer");

        assertThatThrownBy(() -> chatService.answerQuestion(questionId.toString(), request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void answerQuestion_InvalidIdFormat_ThrowsException() {
        QuestionDto.AnswerRequest request = new QuestionDto.AnswerRequest();
        request.setAnswer("Answer");

        assertThatThrownBy(() -> chatService.answerQuestion("not-a-uuid", request, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== deleteQuestion ====================

    @Test
    void deleteQuestion_AuthorCanDelete() {
        UUID questionId = UUID.randomUUID();
        Message questionMsg = Message.builder()
                .id(questionId).userId(userUuid).roomId(roomUuid).roomSlug("questions-general")
                .content("My question").build();

        when(messageRepository.findById(questionId)).thenReturn(Optional.of(questionMsg));
        when(userService.isAdmin(userId)).thenReturn(false);

        chatService.deleteQuestion(questionId.toString(), userId);

        verify(messageRepository).delete(questionMsg);
    }

    @Test
    void deleteQuestion_AdminCanDelete() {
        UUID questionId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        Message questionMsg = Message.builder()
                .id(questionId).userId(otherUser).roomId(roomUuid).roomSlug("questions-general")
                .content("Other's question").build();

        when(messageRepository.findById(questionId)).thenReturn(Optional.of(questionMsg));
        when(userService.isAdmin(userId)).thenReturn(true);

        chatService.deleteQuestion(questionId.toString(), userId);

        verify(messageRepository).delete(questionMsg);
    }

    @Test
    void deleteQuestion_NonAuthorNonAdmin_ThrowsAccessDenied() {
        UUID questionId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        Message questionMsg = Message.builder()
                .id(questionId).userId(otherUser).roomId(roomUuid).roomSlug("questions-general")
                .content("Other's question").build();

        when(messageRepository.findById(questionId)).thenReturn(Optional.of(questionMsg));
        when(userService.isAdmin(userId)).thenReturn(false);

        assertThatThrownBy(() -> chatService.deleteQuestion(questionId.toString(), userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteQuestion_AlsoDeletesAnswers() {
        UUID questionId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        Message questionMsg = Message.builder()
                .id(questionId).userId(userUuid).roomId(roomUuid).roomSlug("questions-general")
                .content("My question").build();
        Message answerMsg = Message.builder()
                .id(answerId).userId(UUID.randomUUID()).roomId(roomUuid)
                .recipientId(questionId).content("Answer").build();

        when(messageRepository.findById(questionId)).thenReturn(Optional.of(questionMsg));
        when(userService.isAdmin(userId)).thenReturn(false);

        chatService.deleteQuestion(questionId.toString(), userId);

        verify(messageRepository).deleteAnswersByRoomAndQuestionId(roomUuid, questionId);
        verify(messageRepository).delete(questionMsg);
    }

    // ==================== getQuestionsByMode ====================

    @Test
    void getQuestionsByMode_RoomExists_ReturnsSortedQuestions() {
        ChatRoom qRoom = ChatRoom.builder().id(roomUuid).slug("questions-nutrition").name("Q").build();
        when(chatRoomRepository.findBySlug("questions-nutrition")).thenReturn(Optional.of(qRoom));

        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();
        Message q1 = Message.builder().id(q1Id).userId(userUuid).userName("User1")
                .content("Q1").roomId(roomUuid).createdAt(LocalDateTime.now().minusHours(2)).build();
        Message q2 = Message.builder().id(q2Id).userId(userUuid).userName("User2")
                .content("Q2").roomId(roomUuid).createdAt(LocalDateTime.now().minusHours(1)).build();
        Message a1 = Message.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).userName("Admin")
                .content("A1").roomId(roomUuid).recipientId(q1Id).createdAt(LocalDateTime.now()).build();

        when(messageRepository.findByRoomIdOrderByCreatedAtAsc(eq(roomUuid), any(Pageable.class)))
                .thenReturn(List.of(q1, q2, a1));

        List<QuestionDto> result = chatService.getQuestionsByMode("nutrition", userId);

        assertThat(result).hasSize(2);
        // Unanswered first
        assertThat(result.get(0).getId()).isEqualTo(q2Id.toString());
        assertThat(result.get(0).isAnswered()).isFalse();
        // Answered second
        assertThat(result.get(1).getId()).isEqualTo(q1Id.toString());
        assertThat(result.get(1).isAnswered()).isTrue();
        assertThat(result.get(1).getAnswer()).isEqualTo("A1");
    }

    @Test
    void getQuestionsByMode_RoomNotFound_ReturnsEmptyList() {
        when(chatRoomRepository.findBySlug("questions-fake")).thenReturn(Optional.empty());

        List<QuestionDto> result = chatService.getQuestionsByMode("fake", userId);

        assertThat(result).isEmpty();
    }

    // ==================== Helpers ====================

    private Message buildMessage(String chatType, String content) {
        return Message.builder()
                .id(UUID.randomUUID())
                .content(content)
                .userId(userUuid)
                .userName("TestUser")
                .chatType(chatType)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
