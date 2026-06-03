package com.platform.model.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminStatsDtoTest {

    @Test
    void adminStatsDto_Builder_SetsAllFields() {
        AdminStatsDto dto = AdminStatsDto.builder()
                .totalUsers(100)
                .totalPosts(250)
                .totalComments(500)
                .totalEvents(30)
                .activeUsers(45)
                .build();

        assertEquals(100, dto.getTotalUsers());
        assertEquals(250, dto.getTotalPosts());
        assertEquals(500, dto.getTotalComments());
        assertEquals(30, dto.getTotalEvents());
        assertEquals(45, dto.getActiveUsers());
    }

    @Test
    void adminStatsDto_NoArgsConstructor_CreatesEmptyObject() {
        AdminStatsDto dto = new AdminStatsDto();

        assertEquals(0, dto.getTotalUsers());
        assertEquals(0, dto.getTotalPosts());
        assertEquals(0, dto.getTotalComments());
        assertEquals(0, dto.getTotalEvents());
        assertEquals(0, dto.getActiveUsers());
    }

    @Test
    void adminStatsDto_AllArgsConstructor_SetsAllFields() {
        AdminStatsDto dto = new AdminStatsDto(10, 20, 30, 40, 50);

        assertEquals(10, dto.getTotalUsers());
        assertEquals(20, dto.getTotalPosts());
        assertEquals(30, dto.getTotalComments());
        assertEquals(40, dto.getTotalEvents());
        assertEquals(50, dto.getActiveUsers());
    }

    @Test
    void adminStatsDto_SettersAndGetters_Work() {
        AdminStatsDto dto = new AdminStatsDto();
        dto.setTotalUsers(5);
        dto.setTotalPosts(15);
        dto.setTotalComments(25);
        dto.setTotalEvents(35);
        dto.setActiveUsers(45);

        assertEquals(5, dto.getTotalUsers());
        assertEquals(15, dto.getTotalPosts());
        assertEquals(25, dto.getTotalComments());
        assertEquals(35, dto.getTotalEvents());
        assertEquals(45, dto.getActiveUsers());
    }
}
