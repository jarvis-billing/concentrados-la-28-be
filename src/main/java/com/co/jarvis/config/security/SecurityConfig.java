package com.co.jarvis.config.security;

import com.co.jarvis.config.security.filters.JwtRequestFilter;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private AccessDeniedException accessDeniedException;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {

        http.exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler((AccessDeniedHandler) accessDeniedException))
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
