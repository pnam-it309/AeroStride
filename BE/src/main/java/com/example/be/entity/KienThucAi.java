package com.example.be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kien_thuc_ai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KienThucAi {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "muc_dich", nullable = false)
    private String mucDich;

    @Column(name = "tu_khoa", columnDefinition = "TEXT")
    private String tuKhoa; // Comma-separated keywords or patterns

    @Column(name = "mau_cau_tra_loi", columnDefinition = "TEXT")
    private String mauCauTraLoi;

    @Column(name = "do_uu_tien")
    private Integer doUuTien;
}
