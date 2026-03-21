package com.example.be.core.security;

import com.example.be.repository.OtpRepository;
import com.example.be.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpRepository otpRepository;

    @Override
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens and OTPs...");
        LocalDateTime now = LocalDateTime.now();
        
        try {
            refreshTokenRepository.deleteByExpiryDateBefore(now);
            log.info("Expired refresh tokens cleared.");
            
            otpRepository.deleteByExpiryDateBefore(now);
            log.info("Expired OTPs cleared.");
            
            log.info("Cleanup of expired tokens completed successfully.");
        } catch (Exception e) {
            log.error("Failed to cleanup expired security data: {}", e.getMessage());
        }
    }
}
