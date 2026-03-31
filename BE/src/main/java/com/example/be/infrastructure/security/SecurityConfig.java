package com.example.be.infrastructure.security;

import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.SecurityConstants;
import com.example.be.infrastructure.security.exception.CustomAccessDeniedHandler;
import com.example.be.infrastructure.security.exception.CustomAuthenticationEntryPoint;
import com.example.be.infrastructure.security.router.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final AuthenticationSecurityConfig authenticationSecurityConfig;
    private final StaffSecurityConfig staffSecurityConfig;
    private final AdminSecurityConfig adminSecurityConfig;
    private final CustomerSecurityConfig customerSecurityConfig;
    private final ExcelSecurityConfig excelSecurityConfig;
    private final TestRedisSecurityConfig testSecurityConfig;
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${allowed.origin}")
    public String ALLOWED_ORIGIN;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        if (ALLOWED_ORIGIN != null && !ALLOWED_ORIGIN.equals("*") && !ALLOWED_ORIGIN.isEmpty()) {
            config.setAllowedOrigins(List.of(ALLOWED_ORIGIN.split(",")));
            config.setAllowCredentials(true);
        } else {
            config.addAllowedOriginPattern("*");
            config.setAllowCredentials(false);
        }
        
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(c -> c.configurationSource(corsConfigurationSource()));
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        http.exceptionHandling(e -> {
            e.accessDeniedHandler(customAccessDeniedHandler);
            e.authenticationEntryPoint(customAuthenticationEntryPoint);
        });

        // Add JWT filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Test configuration
        testSecurityConfig.configure(http);

        // Modular route configurations
        authenticationSecurityConfig.configure(http);
        staffSecurityConfig.configure(http);
        adminSecurityConfig.configure(http);
        customerSecurityConfig.configure(http);
        excelSecurityConfig.configure(http);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll()
                .requestMatchers(RoutesConstant.API_PREFIX + "/**").authenticated()
                .anyRequest().denyAll());
                
        return http.build();
    }

}
