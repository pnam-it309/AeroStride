package com.example.be.infrastructure.security.router;

import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class ExcelSecurityConfig {

    public void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(RoutesConstant.API_PREFIX + "/excel/**").hasAnyRole("QUAN_TRI_VIEN", "NHAN_VIEN")
        );
    }
}
