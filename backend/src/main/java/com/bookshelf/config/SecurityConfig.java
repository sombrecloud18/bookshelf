package com.bookshelf.config;

import com.bookshelf.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/health").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/books/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/books/by-ids").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/collections/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/events/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/recommendations/similar/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/subject-collections").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reviews/book/**").permitAll()
                // Moderator only
                .requestMatchers("/api/admin/**").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/reviews/pending").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/reviews/*/approve").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/reviews/*/reject").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/collections/pending").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/collections/*/approve").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/collections/*/reject").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/subject-collections/pending").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/subject-collections/*/approve").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/subject-collections/*/reject").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/events").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.PUT,  "/api/events/**").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/books").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.PUT,  "/api/books/**").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/users/*/block").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.POST, "/api/users/*/unblock").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/admin/users").hasRole("MODERATOR")
                // All others require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
