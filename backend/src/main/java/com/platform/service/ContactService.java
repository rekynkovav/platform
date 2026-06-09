package com.platform.service;

import com.platform.exception.AccessDeniedException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.Contact;
import com.platform.model.User;
import com.platform.model.dto.ContactDto;
import com.platform.repository.ContactRepository;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public List<ContactDto> getUserContacts(String userId, String search) {
        List<Contact> contacts;

        if (search != null && !search.isEmpty()) {
            contacts = contactRepository.findByUserIdAndContactNameContainingIgnoreCase(userId, search);
        } else {
            contacts = contactRepository.findByUserIdOrderByContactNameAsc(userId);
        }

        Set<UUID> contactUserIds = contacts.stream()
                .map(c -> UUID.fromString(c.getContactUserId()))
                .collect(Collectors.toSet());

        Map<UUID, User> userMap = userRepository.findAllById(contactUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        LocalDateTime now = LocalDateTime.now();

        return contacts.stream()
                .map(c -> toDto(c, userMap, now))
                .collect(Collectors.toList());
    }

    @Transactional
    public ContactDto addContact(String userId, String contactUserId, String nickname) {
        // Проверяем, существует ли пользователь
        User contactUser = userRepository.findById(UUID.fromString(contactUserId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + contactUserId));

        // Проверяем, не добавлен ли уже
        if (contactRepository.existsByUserIdAndContactUserId(userId, contactUserId)) {
            throw new IllegalStateException("Contact already exists");
        }

        Contact contact = Contact.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .contactUserId(contactUserId)
                .contactName(nickname != null ? nickname : contactUser.getUsername())
                .createdAt(LocalDateTime.now())
                .build();

        contact = contactRepository.save(contact);
        log.info("Contact added: {} -> {}", userId, contactUserId);

        Map<UUID, User> userMap = Map.of(contactUser.getId(), contactUser);
        return toDto(contact, userMap, LocalDateTime.now());
    }

    @Transactional
    public void removeContact(String userId, String contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found: " + contactId));

        if (!contact.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to delete this contact");
        }

        contactRepository.delete(contact);
        log.info("Contact removed: {} -> {}", userId, contact.getContactUserId());
    }

    @Transactional
    public ContactDto updateContactNickname(String userId, String contactId, String nickname) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found: " + contactId));

        if (!contact.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to update this contact");
        }

        contact.setContactName(nickname);
        contact.setUpdatedAt(LocalDateTime.now());
        contact = contactRepository.save(contact);

        log.info("Contact nickname updated: {} -> {} by {}", contactId, nickname, userId);

        User contactUser = userRepository.findById(UUID.fromString(contact.getContactUserId())).orElse(null);
        Map<UUID, User> userMap = contactUser != null ? Map.of(contactUser.getId(), contactUser) : Map.of();
        return toDto(contact, userMap, LocalDateTime.now());
    }

    private ContactDto toDto(Contact contact, Map<UUID, User> userMap, LocalDateTime now) {
        User user = userMap.get(UUID.fromString(contact.getContactUserId()));
        return ContactDto.builder()
                .id(contact.getId())
                .userId(contact.getUserId())
                .contactUserId(contact.getContactUserId())
                .contactName(contact.getContactName())
                .status(getUserStatus(user, now))
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .createdAt(contact.getCreatedAt())
                .build();
    }

    private static String getUserStatus(User user, LocalDateTime now) {
        if (user == null || user.getLastActiveAt() == null) return "offline";
        return user.getLastActiveAt().isAfter(now.minusMinutes(5)) ? "online" : "offline";
    }
}
