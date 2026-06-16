package com.platform.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Map;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleUtilsTest {

    @Test
    void getUserRole_WithNullJwt_ReturnsDefaultRole() {
        // Arrange
        Jwt jwt = null;

        // Act
        String result = RoleUtils.getUserRole(jwt);

        // Assert
        assertEquals("USER_0", result);
    }

    @Test
    void getUserRole_WithEmptyRolesClaim_ReturnsDefaultRole() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", java.util.List.of());
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        String result = RoleUtils.getUserRole(jwt);

        // Assert
        assertEquals("USER_0", result);
    }

    @Test
    void getUserRole_WithSingleRole_ReturnsFirstRole() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", java.util.List.of("USER_1"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        String result = RoleUtils.getUserRole(jwt);

        // Assert
        assertEquals("USER_1", result);
    }

    @Test
    void getUserRole_WithMultipleRoles_ReturnsHighestPriorityRole() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", java.util.List.of("USER_2", "CONSULTANT_PITANIE", "MODERATOR_ADMIN"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        String result = RoleUtils.getUserRole(jwt);

        // Assert
        assertEquals("MODERATOR_ADMIN", result);
    }

    @Test
    void getUserRole_WithRolesInMap_ReturnsHighestPriorityRole() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", java.util.List.of("VYACHESLAV", "SUPER_ADMIN"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        String result = RoleUtils.getUserRole(jwt);

        // Assert
        assertEquals("SUPER_ADMIN", result);
    }

    @Test
    void getUserRole_WithNullRealmAccess_ReturnsDefaultRole() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access")).thenReturn(null);

        // Act
        String result = RoleUtils.getUserRole(jwt);

        // Assert
        assertEquals("USER_0", result);
    }

    @Test
    void getUserRole_WithNoRolesKey_ReturnsDefaultRole() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = new HashMap<>();
        // no "roles" key
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        String result = RoleUtils.getUserRole(jwt);

        // Assert
        assertEquals("USER_0", result);
    }

    @Test
    void isAdmin_WithAdminRoles_ReturnsTrue() {
        // Test all admin roles
        assertTrue(RoleUtils.isAdmin("SUPER_ADMIN"));
        assertTrue(RoleUtils.isAdmin("VYACHESLAV"));
        assertTrue(RoleUtils.isAdmin("MODERATOR_ADMIN"));
        assertTrue(RoleUtils.isAdmin("DEVELOPER"));
    }

    @Test
    void isAdmin_WithNonAdminRoles_ReturnsFalse() {
        // Test non-admin roles
        assertFalse(RoleUtils.isAdmin("USER_0"));
        assertFalse(RoleUtils.isAdmin("USER_1"));
        assertFalse(RoleUtils.isAdmin("USER_2"));
        assertFalse(RoleUtils.isAdmin("CONSULTANT_PITANIE"));
        assertFalse(RoleUtils.isAdmin("CONSULTANT_SPORT"));
        assertFalse(RoleUtils.isAdmin(""));
        assertFalse(RoleUtils.isAdmin(null));
    }

    @Test
    void isSuperAdmin_WithSuperAdminRoles_ReturnsTrue() {
        assertTrue(RoleUtils.isSuperAdmin("SUPER_ADMIN"));
        assertTrue(RoleUtils.isSuperAdmin("VYACHESLAV"));
    }

    @Test
    void isSuperAdmin_WithNonSuperAdminRoles_ReturnsFalse() {
        assertFalse(RoleUtils.isSuperAdmin("MODERATOR_ADMIN"));
        assertFalse(RoleUtils.isSuperAdmin("DEVELOPER"));
        assertFalse(RoleUtils.isSuperAdmin("USER_0"));
        assertFalse(RoleUtils.isSuperAdmin(""));
        assertFalse(RoleUtils.isSuperAdmin(null));
    }
}
