package com.platform.util;

import com.platform.enums.UserRole;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility for extracting and checking user roles from JWT tokens.
 * Delegates to {@link UserRole} enum — all role constants live there.
 */
public class RoleUtils {

    /**
     * Extract the highest-priority role from the JWT.
     * Returns the role name string (e.g. "SUPER_ADMIN").
     */
    public static String getUserRole(Jwt jwt) {
        if (jwt == null) {
            return UserRole.USER_0.getValue();
        }

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return UserRole.USER_0.getValue();
        }

        List<String> roles = extractRoles(realmAccess);
        if (roles == null || roles.isEmpty()) {
            return UserRole.USER_0.getValue();
        }

        // Match against enum priority (ordinal order = priority order)
        for (UserRole priorityRole : UserRole.values()) {
            if (roles.contains(priorityRole.getValue())) {
                return priorityRole.getValue();
            }
        }

        return UserRole.USER_0.getValue();
    }

    /**
     * Is this role an admin? (MODERATOR_ADMIN, DEVELOPER, VYACHESLAV, SUPER_ADMIN)
     */
    public static boolean isAdmin(String role) {
        return UserRole.fromString(role).isAdmin();
    }

    /**
     * Is this role a super admin? (SUPER_ADMIN, VYACHESLAV)
     */
    public static boolean isSuperAdmin(String role) {
        return UserRole.fromString(role).isSuperAdmin();
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractRoles(Map<String, Object> realmAccess) {
        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof Collection) {
            return ((Collection<?>) rolesObj).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
