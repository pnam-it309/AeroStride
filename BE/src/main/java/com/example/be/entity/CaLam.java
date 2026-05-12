package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "ca_lam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaLam extends PrimaryEntity {

    @Column(name = "ten_ca")
    private String tenCa;

    @Column(name = "gio_bat_dau")
    private LocalTime gioBatDau;

    @Column(name = "gio_ket_thuc")
    private LocalTime gioKetThuc;

    @Column(name = "mo_ta")
    private String moTa;

    @Builder.Default
    @Column(name = "xoa_mem")
    private Boolean xoaMem = false;
}
