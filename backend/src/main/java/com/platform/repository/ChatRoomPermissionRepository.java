package com.platform.repository;

import com.platform.model.ChatRoomPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomPermissionRepository extends JpaRepository<ChatRoomPermission, Long> {

    List<ChatRoomPermission> findByChatRoomId(UUID roomId);

    Optional<ChatRoomPermission> findByChatRoomIdAndRole(UUID roomId, String role);

    boolean existsByChatRoomIdAndRole(UUID roomId, String role);
}