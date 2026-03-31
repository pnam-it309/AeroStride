package com.example.be.infrastructure.security.router;

import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class AdminSecurityConfig {

    public void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(RoutesConstant.ADMIN + "/**").hasRole("QUAN_TRI_VIEN")
        );
    }
}
