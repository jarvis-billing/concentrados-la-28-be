package com.co.jarvis.config.security;

import com.co.jarvis.config.security.filters.JwtRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private AccessDeniedException accessDeniedException;

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                               org.springframework.security.access.AccessDeniedException accessDeniedException)
                    throws IOException, ServletException {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                Map<String, String> body = Map.of("message", "No tiene permisos para acceder a este recurso.");
                new ObjectMapper().writeValue(response.getOutputStream(), body);
            }
        };
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            Map<String, String> body = Map.of("message", "No está autenticado para acceder a este recurso.");
            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter,
                                                  AccessDeniedHandler customAccessDeniedHandler,
                                                  AuthenticationEntryPoint customAuthenticationEntryPoint) throws Exception {

        http.exceptionHandling(exceptionHandling -> {
                    exceptionHandling
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint);
                })
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(securitySession -> securitySession.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requestAuthorize -> {
                    requestAuthorize
                            .requestMatchers(HttpMethod.POST, "/api/user").permitAll()
                            .requestMatchers("/api/auth/login").permitAll()
                            .anyRequest().authenticated();
                });

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
