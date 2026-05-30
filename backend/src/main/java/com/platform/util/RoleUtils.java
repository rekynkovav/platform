package com.platform.util;

import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import java.util.Arrays;

public class RoleUtils {

    public static String getUserRole(Jwt jwt) {
        if (jwt == null) {
            return "ROLE_USER_0";
        }

        String rolesClaim = jwt.getClaimAsString("realm_access.roles");
        if (rolesClaim == null || rolesClaim.isEmpty()) {
            return "ROLE_USER_0";
        }

        String[] roles = rolesClaim.split(",");
        if (roles.length == 0) {
            return "ROLE_USER_0";
        }

        return roles[0].trim();
    }

    public static boolean isAdmin(String role) {
        return "SUPER_ADMIN".equals(role) ||
               "VYACHESLAV".equals(role) ||
               "MODERATOR_ADMIN".equals(role) ||
               "DEVELOPER".equals(role);
    }

    public static boolean isSuperAdmin(String role) {
        return "SUPER_ADMIN".equals(role) || "VYACHESLAV".equals(role);
    }
}