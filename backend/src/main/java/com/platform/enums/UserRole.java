package com.platform.enums;

import java.util.Arrays;

/**
 * Centralized role enum for the platform.
 * All role checks MUST use this enum — no magic strings.
 *
 * Main hierarchy (high → low):
 *   SUPER_ADMIN > VYACHESLAV > MODERATOR_ADMIN >
 *   CONSULTANT_SPORT = CONSULTANT_PITANIE > USER_2 > USER_1 > USER_0
 *
 * Technical branch (separate):
 *   DEVELOPER > USER_2 (content view only, NO admin/content/broadcast)
 */
public enum UserRole {

    SUPER_ADMIN(true, true),
    VYACHESLAV(true, true),
    MODERATOR_ADMIN(true, false),
    DEVELOPER(true, false),
    CONSULTANT_SPORT(false, false),
    CONSULTANT_PITANIE(false, false),
    PREMIUM(false, false),
    USER_2(false, false),
    USER_1(false, false),
    USER_0(false, false);

    private final boolean admin;
    private final boolean superAdmin;

    UserRole(boolean admin, boolean superAdmin) {
        this.admin = admin;
        this.superAdmin = superAdmin;
    }

    /**
     * Parse role string to enum. Returns USER_0 for unknown/null.
     */
    public static UserRole fromString(String role) {
        if (role == null || role.isBlank()) return USER_0;
        try {
            return valueOf(role);
        } catch (IllegalArgumentException e) {
            return USER_0;
        }
    }

    /**
     * Is this role an admin? (MODERATOR_ADMIN and above)
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * Is this role a super admin? (SUPER_ADMIN, VYACHESLAV)
     */
    public boolean isSuperAdmin() {
        return superAdmin;
    }

    /**
     * Get the String value for use in queries / JWT claims.
     */
    public String getValue() {
        return name();
    }

    /**
     * Get the full ordered list of role names (highest priority first).
     */
    public static String[] allRoleNames() {
        return Arrays.stream(values())
                .map(UserRole::name)
                .toArray(String[]::new);
    }
}
