package com.example.be.repository;

import com.example.be.entity.RefreshToken;
import com.example.be.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RefreshTokenRepository extends BaseRepository<RefreshToken, String> {

    /**
     * Delete tokens where expiry date is before the given time.
     * Uses Spring Data JPA method name derivation to avoid @Query.
     */
    void deleteByExpiryDateBefore(LocalDateTime now);
}
