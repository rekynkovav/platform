package com.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    // @Value("${keycloak.auth-server-url}")
    private String serverUrl = "http://keycloak:8080";

    // @Value("${keycloak.realm}")
    private String realm = "platform";

    // @Value("${keycloak.resource}")
    private String clientId = "platform-backend";

    // @Value("${keycloak.credentials.secret}")
    private String clientSecret = "test-secret-key";

    private Keycloak keycloak;

    private Keycloak getKeycloak() {
        if (keycloak == null) {
            keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .build();
        }
        return keycloak;
    }

    /**
     * Обновление роли пользователя
     */
    public void updateUserRole(String userId, String oldRole, String newRole) {
        try {
            RealmResource realmResource = getKeycloak().realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            RoleRepresentation oldRoleRep = realmResource.roles().get(oldRole).toRepresentation();
            RoleRepresentation newRoleRep = realmResource.roles().get(newRole).toRepresentation();

            userResource.roles().realmLevel().remove(Collections.singletonList(oldRoleRep));
            userResource.roles().realmLevel().add(Collections.singletonList(newRoleRep));

            log.info("User role updated: {} from {} to {}", userId, oldRole, newRole);

        } catch (Exception e) {
            log.error("Failed to update user role in Keycloak", e);
            throw new RuntimeException("Failed to update user role: " + e.getMessage());
        }
    }

    /**
     * Добавление роли пользователю
     */
    public void addUserRole(String userId, String roleName) {
        try {
            RealmResource realmResource = getKeycloak().realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            RoleRepresentation roleRep = realmResource.roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(roleRep));

            log.info("Role added to user: {} -> {}", userId, roleName);

        } catch (Exception e) {
            log.error("Failed to add role to user in Keycloak", e);
            throw new RuntimeException("Failed to add role: " + e.getMessage());
        }
    }

    /**
     * Получение ролей пользователя
     */
    public List<String> getUserRoles(String userId) {
        try {
            RealmResource realmResource = getKeycloak().realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            return userResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .toList();

        } catch (Exception e) {
            log.error("Failed to get user roles from Keycloak", e);
            throw new RuntimeException("Failed to get user roles: " + e.getMessage());
        }
    }
}
