package com.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты
                        .requestMatchers("/actuator/**", "/health", "/ws/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // Защита медиа контента
                        .requestMatchers("/api/media/stream/**").hasAnyRole("USER_0", "USER_1", "USER_2", "MODERATOR_ADMIN", "SUPER_ADMIN", "CONSULTANT_PITANIE", "CONSULTANT_SPORT", "DEVELOPER", "VYACHESLAV")
                        .requestMatchers("/api/media/key/**").hasAnyRole("USER_0", "USER_1", "USER_2", "MODERATOR_ADMIN", "SUPER_ADMIN", "CONSULTANT_PITANIE", "CONSULTANT_SPORT", "DEVELOPER", "VYACHESLAV")
                        .requestMatchers("/api/media/download/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/media/upload/**").hasAnyRole("MODERATOR_ADMIN", "SUPER_ADMIN", "CONSULTANT_PITANIE", "CONSULTANT_SPORT", "DEVELOPER", "VYACHESLAV")

                        // Админка
                        .requestMatchers("/api/admin/**").hasAnyRole("MODERATOR_ADMIN", "SUPER_ADMIN", "VYACHESLAV")

                        // Пользователи
                        .requestMatchers("/api/users/**").authenticated()

                        // Защищённые эндпоинты
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Keycloak хранит роли в realm_access.roles
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        // Извлекаем username из preferred_username
        jwtConverter.setPrincipalClaimName("preferred_username");

        return jwtConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ТОЛЬКО РАЗРЕШЁННЫЕ ДОМЕНЫ (без звёздочки)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",           // Локальная разработка
            "http://localhost:3001",           // Альтернативный порт
            "https://platform.example.com",    // Продакшен (замените на свой домен)
            "https://admin.platform.example.com" // Админка
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);  // 1 час кэширования preflight запросов

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}