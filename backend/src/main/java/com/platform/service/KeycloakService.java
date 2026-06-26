package com.platform.service;

import com.platform.exception.KeycloakException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import jakarta.annotation.PostConstruct;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    @Lazy
    @Autowired
    private KeycloakRetryProcessor retryProcessor;

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private Keycloak keycloak;

    @PostConstruct
    public void validateConfig() {
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("KEYCLOAK_SECRET is not configured. Set keycloak.credentials.secret property");
        }
        if (serverUrl == null || serverUrl.isBlank()) {
            throw new IllegalStateException("Keycloak auth-server-url is not configured");
        }
    }

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
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
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
            throw new KeycloakException("Failed to update user role: " + e.getMessage());
        }
    }

    @Recover
    public void recoverUpdateUserRole(Exception e, String userId, String oldRole, String newRole) {
        log.error("Failed to update user role after retries: {}", userId, e);
        retryProcessor.enqueue("UPDATE_USER_ROLE",
            Map.of("userId", userId, "oldRole", oldRole, "newRole", newRole),
            e.getMessage());
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
            throw new KeycloakException("Failed to add role: " + e.getMessage());
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
            throw new KeycloakException("Failed to get user roles: " + e.getMessage());
        }
    }
}
