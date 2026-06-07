package com.platform.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Контроллер дашборда для фронтенда.
 * Хранит конфигурацию меню дашборда.
 *
 * Frontend вызывает:
 *   GET  /api/dashboard/menu
 *   POST /api/dashboard/menu
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    /**
     * Получить данные меню дашборда
     * Frontend: api.get('/dashboard/menu')
     */
    @GetMapping("/menu")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<List<Map<String, Object>>> getMenuData(
            @AuthenticationPrincipal Jwt jwt) {

        log.debug("Getting dashboard menu data for user: {}", jwt.getSubject());

        // Заглушка — возвращаем пустой список, реальная реализация через БД или конфиг
        return ResponseEntity.ok(List.of());
    }

    /**
     * Сохранить данные меню дашборда
     * Frontend: api.post('/dashboard/menu', data)
     */
    @PostMapping("/menu")
    @PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")
    public ResponseEntity<Map<String, Object>> saveMenuData(
            @RequestBody List<Map<String, Object>> menuData,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Saving dashboard menu data by user: {}, items: {}", jwt.getSubject(), menuData.size());

        // Заглушка — принимаем данные, но пока не сохраняем
        return ResponseEntity.ok(Map.of(
                "saved", true,
                "itemsCount", menuData.size()
        ));
    }
}
