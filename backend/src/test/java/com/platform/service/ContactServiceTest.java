package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Contact;
import com.platform.model.User;
import com.platform.model.dto.ContactDto;
import com.platform.repository.ContactRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock private ContactRepository contactRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ContactService contactService;

    private String userId;
    private String contactUserId;
    private UUID contactUserUuid;
    private User contactUser;
    private Contact contact;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        contactUserId = UUID.randomUUID().toString();
        contactUserUuid = UUID.fromString(contactUserId);

        contactUser = User.builder()
                .id(contactUserUuid)
                .username("contactuser")
                .avatarUrl("https://example.com/avatar.png")
                .lastActiveAt(LocalDateTime.now())
                .build();

        contact = Contact.builder()
                .id("contact-1")
                .userId(userId)
                .contactUserId(contactUserId)
                .contactName("My Friend")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== getUserContacts ====================

    @Test
    void getUserContacts_NoSearch_ReturnsAllContacts() {
        when(contactRepository.findByUserIdOrderByContactNameAsc(userId))
                .thenReturn(List.of(contact));
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));

        List<ContactDto> result = contactService.getUserContacts(userId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContactName()).isEqualTo("My Friend");
    }

    @Test
    void getUserContacts_WithSearch_ReturnsFiltered() {
        when(contactRepository.findByUserIdAndContactNameContainingIgnoreCase(userId, "friend"))
                .thenReturn(List.of(contact));
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));

        List<ContactDto> result = contactService.getUserContacts(userId, "friend");

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserContacts_EmptyResult_ReturnsEmptyList() {
        when(contactRepository.findByUserIdOrderByContactNameAsc(userId))
                .thenReturn(List.of());

        List<ContactDto> result = contactService.getUserContacts(userId, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserContacts_SetsOnlineStatus_WhenRecentlyActive() {
        contactUser.setLastActiveAt(LocalDateTime.now().minusMinutes(2));
        when(contactRepository.findByUserIdOrderByContactNameAsc(userId))
                .thenReturn(List.of(contact));
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));

        List<ContactDto> result = contactService.getUserContacts(userId, null);

        assertThat(result.get(0).getStatus()).isEqualTo("online");
    }

    @Test
    void getUserContacts_SetsOfflineStatus_WhenNotRecentlyActive() {
        contactUser.setLastActiveAt(LocalDateTime.now().minusHours(1));
        when(contactRepository.findByUserIdOrderByContactNameAsc(userId))
                .thenReturn(List.of(contact));
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));

        List<ContactDto> result = contactService.getUserContacts(userId, null);

        assertThat(result.get(0).getStatus()).isEqualTo("offline");
    }

    @Test
    void getUserContacts_SetsOfflineStatus_WhenLastActiveNull() {
        contactUser.setLastActiveAt(null);
        when(contactRepository.findByUserIdOrderByContactNameAsc(userId))
                .thenReturn(List.of(contact));
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));

        List<ContactDto> result = contactService.getUserContacts(userId, null);

        assertThat(result.get(0).getStatus()).isEqualTo("offline");
    }

    @Test
    void getUserContacts_IncludesAvatarUrl() {
        when(contactRepository.findByUserIdOrderByContactNameAsc(userId))
                .thenReturn(List.of(contact));
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));

        List<ContactDto> result = contactService.getUserContacts(userId, null);

        assertThat(result.get(0).getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
    }

    // ==================== addContact ====================

    @Test
    void addContact_NewContact_SavesAndReturns() {
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));
        when(contactRepository.existsByUserIdAndContactUserId(userId, contactUserId)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactDto result = contactService.addContact(userId, contactUserId, "Buddy");

        assertThat(result).isNotNull();
        assertThat(result.getContactName()).isEqualTo("Buddy");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getContactUserId()).isEqualTo(contactUserId);
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void addContact_NullNickname_UsesUsername() {
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));
        when(contactRepository.existsByUserIdAndContactUserId(userId, contactUserId)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactDto result = contactService.addContact(userId, contactUserId, null);

        assertThat(result.getContactName()).isEqualTo("contactuser");
    }

    @Test
    void addContact_AlreadyExists_ThrowsException() {
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.of(contactUser));
        when(contactRepository.existsByUserIdAndContactUserId(userId, contactUserId)).thenReturn(true);

        assertThatThrownBy(() -> contactService.addContact(userId, contactUserId, "Dup"))
                .isInstanceOf(IllegalStateException.class);

        verify(contactRepository, never()).save(any());
    }

    @Test
    void addContact_UserNotFound_ThrowsException() {
        when(userRepository.findById(contactUserUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.addContact(userId, contactUserId, "Name"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== removeContact ====================

    @Test
    void removeContact_OwnerCanRemove() {
        when(contactRepository.findById("contact-1")).thenReturn(Optional.of(contact));

        contactService.removeContact(userId, "contact-1");

        verify(contactRepository).delete(contact);
    }

    @Test
    void removeContact_NonOwner_ThrowsAccessDenied() {
        String otherUserId = UUID.randomUUID().toString();
        when(contactRepository.findById("contact-1")).thenReturn(Optional.of(contact));

        assertThatThrownBy(() -> contactService.removeContact(otherUserId, "contact-1"))
                .isInstanceOf(AccessDeniedException.class);

        verify(contactRepository, never()).delete(any());
    }

    @Test
    void removeContact_NotFound_ThrowsException() {
        when(contactRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.removeContact(userId, "nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== updateContactNickname ====================

    @Test
    void updateContactNickname_OwnerCanUpdate() {
        when(contactRepository.findById("contact-1")).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactDto result = contactService.updateContactNickname(userId, "contact-1", "New Name");

        assertThat(result.getContactName()).isEqualTo("New Name");
        assertThat(contact.getUpdatedAt()).isNotNull();
        verify(contactRepository).save(contact);
    }

    @Test
    void updateContactNickname_NonOwner_ThrowsAccessDenied() {
        String otherUserId = UUID.randomUUID().toString();
        when(contactRepository.findById("contact-1")).thenReturn(Optional.of(contact));

        assertThatThrownBy(() -> contactService.updateContactNickname(otherUserId, "contact-1", "Hack"))
                .isInstanceOf(AccessDeniedException.class);

        verify(contactRepository, never()).save(any());
    }

    @Test
    void updateContactNickname_NotFound_ThrowsException() {
        when(contactRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.updateContactNickname(userId, "nonexistent", "Name"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
