package com.platform.util;

import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoleUtils {

    private static final List<String> ROLE_PRIORITY = List.of(
        "SUPER_ADMIN",
        "VYACHESLAV",
        "DEVELOPER",
        "MODERATOR_ADMIN",
        "CONSULTANT_SPORT",
        "CONSULTANT_PITANIE",
        "PREMIUM",
        "USER_2",
        "USER_1",
        "USER_0"
    );

    private static final List<String> ADMIN_ROLES = List.of(
        "SUPER_ADMIN",
        "VYACHESLAV",
        "DEVELOPER",
        "MODERATOR_ADMIN"
    );

    private static final List<String> SUPER_ADMIN_ROLES = List.of(
        "SUPER_ADMIN",
        "VYACHESLAV"
    );

    public static String getUserRole(Jwt jwt) {
        if (jwt == null) {
            return "USER_0";
        }

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return "USER_0";
        }

        List<String> roles = extractRoles(realmAccess);
        if (roles == null || roles.isEmpty()) {
            return "USER_0";
        }

        for (String priorityRole : ROLE_PRIORITY) {
            if (roles.contains(priorityRole)) {
                return priorityRole;
            }
        }

        return "USER_0";
    }

    public static boolean isAdmin(String role) {
        return role != null && ADMIN_ROLES.contains(role);
    }

    public static boolean isSuperAdmin(String role) {
        return role != null && SUPER_ADMIN_ROLES.contains(role);
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
