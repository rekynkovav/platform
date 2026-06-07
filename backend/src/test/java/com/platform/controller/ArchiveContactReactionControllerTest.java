package com.platform.controller;

import com.platform.model.dto.ArchiveDto;
import com.platform.model.dto.ContactDto;
import com.platform.model.dto.ReactionDto;
import com.platform.service.ArchiveService;
import com.platform.service.ContactService;
import com.platform.service.ReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArchiveContactReactionControllerTest {

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .build();
    }

    // ==================== ArchiveController ====================

    @Test
    void archiveGetAll_ReturnsListFromService() {
        ArchiveService archiveService = mock(ArchiveService.class);
        List<ArchiveDto> expected = List.of(new ArchiveDto());
        when(archiveService.getAllArchives(50, null)).thenReturn(expected);

        ArchiveController controller = new ArchiveController(archiveService);
        ResponseEntity<List<ArchiveDto>> response = controller.getAllArchives(50, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void archiveGetById_IncrementsViewsAndReturnsArchive() {
        ArchiveService archiveService = mock(ArchiveService.class);
        ArchiveDto archive = new ArchiveDto();
        when(archiveService.getArchive("arc-1")).thenReturn(archive);

        ArchiveController controller = new ArchiveController(archiveService);
        ResponseEntity<ArchiveDto> response = controller.getArchive("arc-1");

        assertEquals(200, response.getStatusCode().value());
        verify(archiveService).incrementViews("arc-1");
    }

    @Test
    void archiveGetByEvent_ReturnsListFromService() {
        ArchiveService archiveService = mock(ArchiveService.class);
        when(archiveService.getArchivesByEvent("event-1")).thenReturn(List.of());

        ArchiveController controller = new ArchiveController(archiveService);
        ResponseEntity<List<ArchiveDto>> response = controller.getArchivesByEvent("event-1");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void archiveDelete_ReturnsNoContent() {
        ArchiveService archiveService = mock(ArchiveService.class);

        ArchiveController controller = new ArchiveController(archiveService);
        ResponseEntity<Void> response = controller.deleteArchive("arc-1", jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(archiveService).deleteArchive("arc-1", jwt);
    }

    // ==================== ContactController ====================

    @Test
    void contactGetAll_ReturnsListFromService() {
        ContactService contactService = mock(ContactService.class);
        when(contactService.getUserContacts("user-123", "john")).thenReturn(List.of(new ContactDto()));

        ContactController controller = new ContactController(contactService);
        ResponseEntity<List<ContactDto>> response = controller.getContacts("john", jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void contactGetAll_NullSearch_PassesNullToService() {
        ContactService contactService = mock(ContactService.class);
        when(contactService.getUserContacts("user-123", null)).thenReturn(List.of());

        ContactController controller = new ContactController(contactService);
        controller.getContacts(null, jwt);

        verify(contactService).getUserContacts("user-123", null);
    }

    @Test
    void contactRemove_ReturnsNoContent() {
        ContactService contactService = mock(ContactService.class);

        ContactController controller = new ContactController(contactService);
        ResponseEntity<Void> response = controller.removeContact("contact-1", jwt);

        assertEquals(204, response.getStatusCode().value());
        verify(contactService).removeContact("user-123", "contact-1");
    }

    // ==================== ReactionController ====================

    @Test
    void reactionToggle_ReturnsReactionFromService() {
        ReactionService reactionService = mock(ReactionService.class);
        ReactionDto expected = new ReactionDto();
        when(reactionService.toggleReaction("post-1", "like", jwt)).thenReturn(expected);

        ReactionController controller = new ReactionController(reactionService);
        ResponseEntity<ReactionDto> response = controller.toggleReaction("post-1", "like", jwt);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void reactionGetStatus_ReturnsReactionFromService() {
        ReactionService reactionService = mock(ReactionService.class);
        ReactionDto expected = new ReactionDto();
        when(reactionService.getReactionStatus("post-1", jwt)).thenReturn(expected);

        ReactionController controller = new ReactionController(reactionService);
        ResponseEntity<ReactionDto> response = controller.getReactionStatus("post-1", jwt);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }
}
