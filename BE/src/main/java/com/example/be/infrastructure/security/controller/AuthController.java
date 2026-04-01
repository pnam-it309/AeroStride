package com.example.be.infrastructure.security.controller;

import com.example.be.entity.RefreshToken;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.security.JwtTokenProvider;
import com.example.be.infrastructure.security.dto.AuthResponse;
import com.example.be.infrastructure.security.dto.LoginRequest;
import com.example.be.infrastructure.security.dto.TokenRefreshRequest;
import com.example.be.infrastructure.security.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.AUTH)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("LOGIN ATTEMPT: User [{}] with password length [{}]", loginRequest.getUsername(), 
                loginRequest.getPassword() != null ? loginRequest.getPassword().length() : 0);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .username(userDetails.getUsername())
                .role(role)
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(token -> {
                    String username = refreshTokenService.getUsernameFromToken(token);
                    String newAccessToken = jwtTokenProvider.generateToken(username);
                    return ResponseEntity.ok(AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(request.getRefreshToken())
                            .username(username)
                            .build());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null) {
            refreshTokenService.deleteByUsername(authentication.getName());
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
