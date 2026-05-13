package com.platform.repository;

import com.platform.model.ChatRoomPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomPermissionRepository extends JpaRepository<ChatRoomPermission, Long> {

    List<ChatRoomPermission> findByChatRoomId(Long roomId);

    Optional<ChatRoomPermission> findByChatRoomIdAndRole(Long roomId, String role);

    boolean existsByChatRoomIdAndRole(Long roomId, String role);
}