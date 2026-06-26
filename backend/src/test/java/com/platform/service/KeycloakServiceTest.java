package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты для KeycloakService.
 * Поскольку KeycloakService напрямую создаёт Keycloak-клиент через KeycloakBuilder
 * (без инъекции), полноценное мокание требует PowerMock/ReflectionTestUtils.
 * Здесь тестируем поведение при недоступности Keycloak (ошибки подключения).
 */
@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @Mock
    private KeycloakRetryProcessor retryProcessor;

    @InjectMocks
    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        // retryProcessor is injected by @Mock + @InjectMocks
    }

    @Test
    void updateUserRole_KeycloakUnavailable_ThrowsRuntimeException() {
        // Keycloak не запущен в unit-тестах, поэтому ожидаем RuntimeException
        assertThatThrownBy(() -> keycloakService.updateUserRole("user-1", "USER_0", "USER_1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void addUserRole_KeycloakUnavailable_ThrowsRuntimeException() {
        assertThatThrownBy(() -> keycloakService.addUserRole("user-1", "USER_1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getUserRoles_KeycloakUnavailable_ThrowsRuntimeException() {
        assertThatThrownBy(() -> keycloakService.getUserRoles("user-1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void recoverUpdateUserRole_DoesNotThrow() {
        // Recover-метод должен отрабатывать без исключений
        keycloakService.recoverUpdateUserRole(
                new RuntimeException("test"), "user-1", "USER_0", "USER_1");
        // Если дошли сюда — тест пройден
    }
}
