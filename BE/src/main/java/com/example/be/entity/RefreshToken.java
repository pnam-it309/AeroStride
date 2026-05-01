package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * RefreshToken - Stores JWT refresh tokens.
 * Can be linked to either KhachHang or NhanVien (only one will be non-null).
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "token", unique = true, nullable = false)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Link to KhachHang (nullable - only set if token belongs to a customer).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    /**
     * Link to NhanVien (nullable - only set if token belongs to a staff/admin).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

}
