package com.example.BookWhiz.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── Public ────────────────────────────────────────────
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/partner/apply").permitAll()
                .requestMatchers("/api/partner/status/**").permitAll()
                .requestMatchers("/api/permissions/public").permitAll()
                .requestMatchers("/ws/**").permitAll()

                // Per-user permission map — any logged-in user can fetch their own
                .requestMatchers("/api/permissions/me").authenticated()
                .requestMatchers("/api/permissions/user/**").authenticated()

                // ── Notifications ─────────────────────────────────────
                .requestMatchers("/api/notifications/**").authenticated()

                // ── USER ──────────────────────────────────────────────
                .requestMatchers("/api/user/**")
                    .hasAnyRole("USER", "ADMIN", "MANAGER", "SUPER_ADMIN")

                // ── MANAGER / THEATER OWNER / PARTNER ─────────────────
                .requestMatchers("/api/partner-portal/**")
                    .hasAnyRole("MANAGER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/theater-owner/**")
                    .hasAnyRole("MANAGER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/manager/**")
                    .hasAnyRole("MANAGER", "ADMIN", "SUPER_ADMIN")

                // ── ADMIN ─────────────────────────────────────────────
                .requestMatchers("/api/admin/requests/**")
                    .hasAnyRole("ADMIN", "MANAGER", "SUPER_ADMIN")
                // Admin per-user permission management
                .requestMatchers("/api/admin/permissions/users/**")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/admin/permissions/user/**")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/admin/**")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")

                // ── Custom Roles ─────────────────────────────────────
                .requestMatchers("/api/roles/public").permitAll()
                .requestMatchers("/api/roles/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/superadmin/roles/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/superadmin/roles/**").hasRole("SUPER_ADMIN")
                // ── SUPERADMIN — specific rules BEFORE the catch-all ──
                // Approval requests — ADMIN read, SUPER_ADMIN write
                .requestMatchers(HttpMethod.GET,  "/api/superadmin/requests/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/superadmin/requests/**")
                    .hasRole("SUPER_ADMIN")

                // Partner applications — ADMIN can act
                .requestMatchers("/api/superadmin/partners/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Analytics & stats — ADMIN can view
                .requestMatchers(HttpMethod.GET, "/api/superadmin/analytics/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/superadmin/revenue")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/superadmin/revenue/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/superadmin/stats")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Movies, Shows, Venues — ADMIN can CRUD
                .requestMatchers("/api/superadmin/movies/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/superadmin/shows/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/superadmin/venues/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Users — ADMIN can view
                .requestMatchers(HttpMethod.GET, "/api/superadmin/users/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Per-user permission endpoints — SUPER_ADMIN only
                .requestMatchers("/api/superadmin/permissions/users/**")
                    .hasRole("SUPER_ADMIN")
                .requestMatchers("/api/superadmin/permissions/user/**")
                    .hasRole("SUPER_ADMIN")

                // Role-level permissions — SUPER_ADMIN only
                .requestMatchers("/api/superadmin/permissions/**")
                    .hasRole("SUPER_ADMIN")

                // ⚠️ CATCH-ALL — must be last among superadmin rules
                .requestMatchers("/api/superadmin/**")
                    .hasRole("SUPER_ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}