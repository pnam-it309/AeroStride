package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "phuong_thuc_thanh_toan")
@Getter
@Setter
@NoArgsConstructor
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_phuong_thuc_thanh_toan")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_phuong_thuc_thanh_toan"))
})
public class PhuongThucThanhToan extends BaseCodeNameEntity {

    @Builder
    public PhuongThucThanhToan(String ma, String ten) {
        this.setMa(ma);
        this.setTen(ten);
    }
}
