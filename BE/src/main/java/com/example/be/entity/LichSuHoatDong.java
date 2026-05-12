package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lich_su_hoat_dong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuHoatDong extends PrimaryEntity {

    @Column(name = "hanh_dong")
    private String hanhDong;

    @Column(name = "doi_tuong")
    private String doiTuong;
}
