package com.platform.service;

import com.platform.exception.ResourceNotFoundException;
import com.platform.model.ChatRoom;
import com.platform.model.ChatRoomPermission;
import com.platform.model.dto.ChatRoomDto;
import com.platform.repository.ChatRoomRepository;
import com.platform.repository.ChatRoomPermissionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomPermissionRepository chatRoomPermissionRepository;

    @PostConstruct
    public void initDefaultRooms() {
        // Временно отключено из-за проблем с JSONB
        System.out.println("⚠️ Инициализация комнат временно отключена из-за проблем с JSONB");
    }

    private void createIfNotExists(String slug, String type, String name) {
        if (!chatRoomRepository.existsBySlug(slug)) {
            ChatRoom room = ChatRoom.builder()
                    .slug(slug)
                    .type(type)
                    .name(name)
                    .isActive(true)
                    // .settings("{}")
                    .build();
            chatRoomRepository.save(room);
        }
    }

    public ChatRoom getBySlug(String slug) {
        return chatRoomRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + slug));
    }

    public ChatRoom findBySlug(String slug) {
        return chatRoomRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + slug));
    }

    public List<ChatRoom> getAllActiveRooms() {
        return chatRoomRepository.findAll().stream()
                .filter(ChatRoom::getIsActive)
                .toList();
    }

    /**
     * Одноразовый backfill после запуска (вызывается из @PostConstruct)
     */
    public void backfillExistingMessages() {
        // Выполняется через Liquibase миграцию выше
        // Здесь можно добавить логику, если нужно
        System.out.println("✅ Backfill room_id выполнен через Liquibase (v5/03-backfill-room-id)");
    }

    /**
     * Получить только комнаты вопросов (для вкладки Помощь)
     */
    public List<ChatRoom> getQuestionRooms() {
        return chatRoomRepository.findAll().stream()
                .filter(room -> room.getType().equals("question") && room.getIsActive())
                .toList();
    }

    /**
     * Получить комнату вопроса по slug (для фронта HelpCenter)
     */
    public ChatRoom getQuestionRoomByMode(String mode) {
        String slug = switch (mode) {
            case "general" -> "questions-general";
            case "nutrition" -> "questions-nutrition";
            case "vyacheslav" -> "questions-vyacheslav";
            default -> "questions-general";
        };
        return getBySlug(slug);
    }

    /**
     * Возвращает DTO-версию комнат вопросов (для фронта HelpCenter)
     */
    public List<ChatRoomDto> getQuestionRoomsDto() {
        return getQuestionRooms().stream()
                .map(room -> ChatRoomDto.builder()
                        .id(room.getId() != null ? (long) room.getId().hashCode() : null)
                        .slug(room.getSlug())
                        .type(room.getType())
                        .name(room.getName())
                        .description(room.getDescription())
                        .isActive(room.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Установить права для роли в комнате
     */
    public void setPermission(UUID roomId, String role, String permissionLevel) {
        ChatRoomPermission permission = chatRoomPermissionRepository.findByChatRoomIdAndRole(roomId, role)
                .orElse(ChatRoomPermission.builder()
                        .chatRoom(chatRoomRepository.findById(roomId).orElseThrow())
                        .role(role)
                        .permissionLevel(permissionLevel)
                        .build());
        permission.setPermissionLevel(permissionLevel);
        chatRoomPermissionRepository.save(permission);
    }

    /**
     * Проверить, имеет ли роль нужный уровень доступа к комнате
     */
    public boolean hasPermission(UUID roomId, String role, String requiredLevel) {
        try {
            log.debug("Checking permission for roomId: {}, role: {}, requiredLevel: {}", roomId, role, requiredLevel);

            if (roomId == null) {
                log.warn("RoomId is null for permission check");
                return false;
            }

            if (role == null || role.isEmpty()) {
                log.warn("Role is null or empty for permission check");
                return false;
            }

            return chatRoomPermissionRepository.findByChatRoomIdAndRole(roomId, role)
                    .map(p -> {
                        if ("admin".equals(p.getPermissionLevel())) return true;
                        if ("read_write".equals(p.getPermissionLevel())) return true;
                        return "read".equals(p.getPermissionLevel()) && "read".equals(requiredLevel);
                    })
                    .orElse(false); // по умолчанию доступ закрыт

        } catch (Exception e) {
            log.error("Error checking permission for roomId {}: {}", roomId, e.getMessage(), e);
            return false; // В случае ошибки возвращаем false для безопасности
        }
    }

    /**
     * Инициализация дефолтных прав доступа для всех комнат
     * Вызывается один раз при старте приложения
     */
    public void initDefaultPermissions() {
        // Общие вопросы — все пользователи могут читать и писать
        setPermissionForRoom("questions-general", "ROLE_USER_0", "read_write");
        setPermissionForRoom("questions-general", "ROLE_CONSULTANT_PITANIE", "admin");
        setPermissionForRoom("questions-general", "ROLE_VYACHESLAV", "admin");
        setPermissionForRoom("questions-general", "ROLE_MODERATOR_ADMIN", "admin");

        // Питание — только консультант питания + Vyacheslav
        setPermissionForRoom("questions-nutrition", "ROLE_CONSULTANT_PITANIE", "admin");
        setPermissionForRoom("questions-nutrition", "ROLE_VYACHESLAV", "admin");
        setPermissionForRoom("questions-nutrition", "ROLE_USER_0", "read_write");

        // Лично Вячеславу — только Vyacheslav + админы
        setPermissionForRoom("questions-vyacheslav", "ROLE_VYACHESLAV", "admin");
        setPermissionForRoom("questions-vyacheslav", "ROLE_MODERATOR_ADMIN", "admin");
        setPermissionForRoom("questions-vyacheslav", "ROLE_USER_0", "read_write");

        // Обычные чаты (user1, user2, nutrition)
        setPermissionForRoom("user1", "ROLE_USER_0", "read_write");
        setPermissionForRoom("user2", "ROLE_USER_0", "read_write");
        setPermissionForRoom("nutrition", "ROLE_USER_0", "read_write");
        setPermissionForRoom("nutrition", "ROLE_CONSULTANT_PITANIE", "admin");

        System.out.println("✅ Default room permissions initialized successfully");
    }

    private void setPermissionForRoom(String roomSlug, String role, String permissionLevel) {
        ChatRoom room = getBySlug(roomSlug);
        if (!chatRoomPermissionRepository.existsByChatRoomIdAndRole(room.getId(), role)) {
            ChatRoomPermission perm = ChatRoomPermission.builder()
                    .chatRoom(room)
                    .role(role)
                    .permissionLevel(permissionLevel)
                    .build();
            chatRoomPermissionRepository.save(perm);
        }
    }
}