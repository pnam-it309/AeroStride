package com.example.be.core.admin.nhanvien.model.request;

import com.example.be.entity.NhanVien;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    private Integer id; // ✅ Tự sinh khi save

    private NhanVien nhanVien;

    private String email;

    private LocalDateTime requestedAt;

    private Status status;

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}
