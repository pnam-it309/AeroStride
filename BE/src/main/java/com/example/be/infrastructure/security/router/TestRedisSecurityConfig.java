package com.example.be.infrastructure.security.router;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class TestRedisSecurityConfig {

    public void configure(HttpSecurity http) throws Exception {
        // Permit access to test endpoints if any
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/test/**").permitAll()
        );
    }
}
