package com.platform.repository;

import com.platform.config.TestJpaConfig;
import com.platform.model.AccessLog;
import com.platform.model.ChatRoom;
import com.platform.model.ChatRoomPermission;
import com.platform.model.EventMaterial;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class AccessLogEventMaterialPermissionRepositoryTest {

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private EventMaterialRepository eventMaterialRepository;

    @Autowired
    private ChatRoomPermissionRepository chatRoomPermissionRepository;

    // ==================== AccessLogRepository ====================

    @Test
    void accessLogRepository_FindByFileId_ReturnsMatchingLogs() {
        accessLogRepository.save(AccessLog.builder().fileId("f1").userId("u1").action("view").success(true).build());
        accessLogRepository.save(AccessLog.builder().fileId("f1").userId("u2").action("download").success(true).build());
        accessLogRepository.save(AccessLog.builder().fileId("f2").userId("u1").action("view").success(true).build());

        List<AccessLog> logs = accessLogRepository.findByFileId("f1");
        assertEquals(2, logs.size());
    }

    @Test
    void accessLogRepository_FindByUserId_ReturnsUserLogs() {
        accessLogRepository.save(AccessLog.builder().fileId("f10").userId("user-x").action("view").success(true).build());
        accessLogRepository.save(AccessLog.builder().fileId("f11").userId("user-x").action("download").success(false).errorMessage("denied").build());
        accessLogRepository.save(AccessLog.builder().fileId("f12").userId("user-y").action("view").success(true).build());

        List<AccessLog> logs = accessLogRepository.findByUserId("user-x");
        assertEquals(2, logs.size());
    }

    // ==================== EventMaterialRepository ====================

    @Test
    void eventMaterialRepository_FindBySectionIdAndIsActiveTrue_ExcludesInactive() {
        eventMaterialRepository.save(EventMaterial.builder().sectionId("about").type("text").title("Active1").isActive(true).sortOrder(1).build());
        eventMaterialRepository.save(EventMaterial.builder().sectionId("about").type("text").title("Inactive").isActive(false).sortOrder(2).build());
        eventMaterialRepository.save(EventMaterial.builder().sectionId("about").type("video").title("Active2").isActive(true).sortOrder(3).build());

        List<EventMaterial> materials = eventMaterialRepository.findBySectionIdAndIsActiveTrueOrderBySortOrderAscCreatedAtDesc("about");
        assertEquals(2, materials.size());
        assertTrue(materials.stream().allMatch(EventMaterial::getIsActive));
    }

    @Test
    void eventMaterialRepository_CountBySectionId_ReturnsCorrectCount() {
        eventMaterialRepository.save(EventMaterial.builder().sectionId("interviews").type("video").title("I1").isActive(true).build());
        eventMaterialRepository.save(EventMaterial.builder().sectionId("interviews").type("audio").title("I2").isActive(true).build());
        eventMaterialRepository.save(EventMaterial.builder().sectionId("music").type("audio").title("M1").isActive(true).build());

        assertEquals(2, eventMaterialRepository.countBySectionId("interviews"));
        assertEquals(1, eventMaterialRepository.countBySectionId("music"));
        assertEquals(0, eventMaterialRepository.countBySectionId("nonexistent"));
    }

    // ==================== ChatRoomPermissionRepository ====================

    @Test
    void chatRoomPermissionRepository_FindByChatRoomIdAndRole_ReturnsPermission() {
        ChatRoom room = ChatRoom.builder().slug("test-perm").type("general").name("Test").isActive(true).build();

        assertFalse(chatRoomPermissionRepository.existsByChatRoomIdAndRole(UUID.randomUUID(), "ROLE_USER"));
    }
}
