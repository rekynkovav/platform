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
                        .requestMatchers("/api/media/stream/**").hasAnyRole("ROLE_USER", "ROLE_PREMIUM", "ROLE_ADVANCED", "ROLE_CREATOR", "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_SPORT_ADMIN", "ROLE_NUTRITION_ADMIN", "ROLE_CONSULTANT_ADMIN", "ROLE_OWNER", "ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/media/key/**").hasAnyRole("ROLE_USER", "ROLE_PREMIUM", "ROLE_ADVANCED", "ROLE_CREATOR", "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_SPORT_ADMIN", "ROLE_NUTRITION_ADMIN", "ROLE_CONSULTANT_ADMIN", "ROLE_OWNER", "ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/media/download/**").hasRole("ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/media/upload/**").hasAnyRole("ROLE_CREATOR", "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_SPORT_ADMIN", "ROLE_NUTRITION_ADMIN", "ROLE_CONSULTANT_ADMIN", "ROLE_OWNER", "ROLE_SUPER_ADMIN")

                        // Админка
                        .requestMatchers("/api/admin/**").hasAnyRole("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_SPORT_ADMIN", "ROLE_NUTRITION_ADMIN", "ROLE_CONSULTANT_ADMIN", "ROLE_OWNER", "ROLE_SUPER_ADMIN")

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
        configuration.setAllowedOrigins(List.of("https://platform.com", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}