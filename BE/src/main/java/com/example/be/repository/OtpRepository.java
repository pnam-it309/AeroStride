package com.example.be.repository;

import com.example.be.entity.Otp;
import com.example.be.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OtpRepository extends BaseRepository<Otp, String> {

    /**
     * Delete OTPs where expiry date is before the given time.
     * Uses Spring Data JPA method name derivation to avoid @Query.
     */
    void deleteByExpiryDateBefore(LocalDateTime now);
}
