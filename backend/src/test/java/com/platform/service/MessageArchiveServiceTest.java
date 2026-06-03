package com.platform.service;

import com.platform.model.Message;
import com.platform.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageArchiveServiceTest {

    @Mock private MessageRepository messageRepository;
    @InjectMocks private MessageArchiveService messageArchiveService;

    // ==================== archiveOldMessages ====================

    @Test
    void archiveOldMessages_WhenOldMessagesExist_LogsCount() {
        Message oldMsg1 = Message.builder().id(UUID.randomUUID()).content("old1").createdAt(LocalDateTime.now().minusMonths(7)).build();
        Message oldMsg2 = Message.builder().id(UUID.randomUUID()).content("old2").createdAt(LocalDateTime.now().minusMonths(8)).build();

        when(messageRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(oldMsg1, oldMsg2));

        messageArchiveService.archiveOldMessages();

        verify(messageRepository).findByCreatedAtBefore(any(LocalDateTime.class));
    }

    @Test
    void archiveOldMessages_WhenNoOldMessages_DoesNothing() {
        when(messageRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        messageArchiveService.archiveOldMessages();

        verify(messageRepository).findByCreatedAtBefore(any(LocalDateTime.class));
        verifyNoMoreInteractions(messageRepository);
    }

    @Test
    void archiveOldMessages_UsesCutoffSixMonthsAgo() {
        when(messageRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        LocalDateTime beforeCall = LocalDateTime.now().minusMonths(6);
        messageArchiveService.archiveOldMessages();
        LocalDateTime afterCall = LocalDateTime.now().minusMonths(6);

        verify(messageRepository).findByCreatedAtBefore(argThat(cutoff ->
                !cutoff.isBefore(beforeCall.minusSeconds(5)) && !cutoff.isAfter(afterCall.plusSeconds(5))
        ));
    }
}
