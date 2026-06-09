package com.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты
                        .requestMatchers("/actuator/**", "/health", "/api/health", "/api/health/**", "/ws/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // Защита медиа контента
                        .requestMatchers("/api/media/stream/**").hasAnyRole("USER_0", "USER_1", "USER_2", "MODERATOR_ADMIN", "SUPER_ADMIN", "CONSULTANT_PITANIE", "CONSULTANT_SPORT", "DEVELOPER", "VYACHESLAV")
                        .requestMatchers("/api/media/key/**").hasAnyRole("USER_0", "USER_1", "USER_2", "MODERATOR_ADMIN", "SUPER_ADMIN", "CONSULTANT_PITANIE", "CONSULTANT_SPORT", "DEVELOPER", "VYACHESLAV")
                        .requestMatchers("/api/media/download/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/media/upload/**").hasAnyRole("MODERATOR_ADMIN", "SUPER_ADMIN", "CONSULTANT_PITANIE", "CONSULTANT_SPORT", "DEVELOPER", "VYACHESLAV")

                        // Админка
                        .requestMatchers("/api/admin/**").hasAnyRole("MODERATOR_ADMIN", "SUPER_ADMIN", "VYACHESLAV")

                        // Дашборд (конфигурация меню)
                        .requestMatchers("/api/dashboard/**").hasAnyRole("MODERATOR_ADMIN", "SUPER_ADMIN", "VYACHESLAV")

                        // Демо-эндпоинты (только для авторизованных)
                        .requestMatchers("/api/demo/**").authenticated()

                        // Пользователи
                        .requestMatchers("/api/users/**").authenticated()

                        .requestMatchers("/api/broadcast/create", "/api/broadcast/start", "/api/broadcast/*/end", "/api/broadcast/*/stats", "/api/broadcast/webhook/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_CONSULTANT_PITANIE", "ROLE_CONSULTANT_SPORT", "ROLE_MODERATOR_ADMIN", "ROLE_DEVELOPER", "ROLE_VYACHESLAV")

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
        Converter<Jwt, Collection<GrantedAuthority>> customConverter = jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List) {
                    for (Object role : (List<?>) rolesObj) {
                        String roleStr = role.toString().toUpperCase();
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleStr));
                    }
                }
            }
            return authorities;
        };

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(customConverter);

        // Извлекаем username из preferred_username
        jwtConverter.setPrincipalClaimName("preferred_username");

        return jwtConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
