package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "phan_quyen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_phan_quyen")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_phan_quyen"))
})
public class PhanQuyen extends BaseCodeNameEntity {

    @Column(name = "quyen_han")
    private String quyenHan;

    @Column(name = "mo_ta")
    private String moTa;

}
