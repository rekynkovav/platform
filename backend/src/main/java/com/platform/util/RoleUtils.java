package com.platform.util;

import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;

public class RoleUtils {

    public static String getUserRole(Jwt jwt) {
        if (jwt == null) return "USER_0";

        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles == null || roles.isEmpty()) return "USER_0";

        if (roles.contains("SUPER_ADMIN")) return "SUPER_ADMIN";
        if (roles.contains("VYACHESLAV")) return "VYACHESLAV";
        if (roles.contains("MODERATOR_ADMIN")) return "MODERATOR_ADMIN";
        if (roles.contains("DEVELOPER")) return "DEVELOPER";
        if (roles.contains("CONSULTANT_SPORT")) return "CONSULTANT_SPORT";
        if (roles.contains("CONSULTANT_PITANIE")) return "CONSULTANT_PITANIE";
        if (roles.contains("USER_2")) return "USER_2";
        if (roles.contains("USER_1")) return "USER_1";
        return "USER_0";
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