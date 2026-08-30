package com.platform.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class KeycloakHealthIndicator implements HealthIndicator {

    @Value("${keycloak.auth-server-url:http://keycloak:8080/auth}")
    private String authServerUrl;

    @Value("${keycloak.realm:platform}")
    private String realm;

    @Override
    public Health health() {
        try {
            String healthUrl = authServerUrl + "/realms/" + realm;
            HttpURLConnection connection = (HttpURLConnection) new URL(healthUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            if (responseCode == 200) {
                return Health.up()
                        .withDetail("keycloak", "available")
                        .withDetail("realm", realm)
                        .withDetail("url", authServerUrl)
                        .build();
            } else {
                return Health.down()
                        .withDetail("keycloak", "unreachable")
                        .withDetail("httpStatus", responseCode)
                        .withDetail("url", authServerUrl)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("keycloak", "unavailable")
                    .withDetail("error", e.getMessage())
                    .withDetail("url", authServerUrl)
                    .build();
        }
    }
}
