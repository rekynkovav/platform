package com.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.platform.enums.UserRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.core.convert.converter.Converter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!local")
public class SecurityConfig {

    @Value("${security.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // Encode the CSRF token in a way that doesn't break JavaScript (XOR masking)
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        // Disable CSRF for stateless JWT Bearer token endpoints
                        // Browsers don't auto-send Authorization headers cross-origin,
                        // so CSRF is not a threat for Bearer token auth.
                        .disable()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/health", "/api/health", "/api/health/**").permitAll()
                        .requestMatchers("/api/broadcast/webhook/**").permitAll()  // RTMP webhooks from nginx-rtmp (no JWT)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/upcoming", "/api/events/next", "/api/events/past").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/{id}").permitAll()

                        .requestMatchers("/api/media/stream/**").hasAnyRole(UserRole.ALL_ROLES)
                        .requestMatchers("/api/media/key/**").hasAnyRole(UserRole.ALL_ROLES)
                        .requestMatchers("/api/media/download/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/media/upload/**").hasAnyRole(UserRole.CONTENT_ROLES)

                        .requestMatchers("/api/admin/**").hasAnyRole(UserRole.ADMIN_ROLES)

                        .requestMatchers("/api/dashboard/**").hasAnyRole(UserRole.ADMIN_ROLES)

                        .requestMatchers("/api/demo/**").authenticated()

                        .requestMatchers("/api/users/**").authenticated()

                        .requestMatchers("/api/broadcast/create", "/api/broadcast/start", "/api/broadcast/*/end", "/api/broadcast/*/stats").hasAnyAuthority(UserRole.BROADCAST_ROLES_AUTHORITY)

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy(
            "ROLE_SUPER_ADMIN > ROLE_VYACHESLAV\n" +
            "ROLE_VYACHESLAV > ROLE_MODERATOR_ADMIN\n" +
            "ROLE_MODERATOR_ADMIN > ROLE_CONSULTANT_SPORT\n" +
            "ROLE_MODERATOR_ADMIN > ROLE_CONSULTANT_PITANIE\n" +
            "ROLE_CONSULTANT_SPORT > ROLE_USER_2\n" +
            "ROLE_CONSULTANT_PITANIE > ROLE_USER_2\n" +
            "ROLE_USER_2 > ROLE_USER_1\n" +
            "ROLE_USER_1 > ROLE_USER_0\n" +
            "ROLE_DEVELOPER > ROLE_USER_2"
        );
        return roleHierarchy;
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

        jwtConverter.setPrincipalClaimName("preferred_username");

        return jwtConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
