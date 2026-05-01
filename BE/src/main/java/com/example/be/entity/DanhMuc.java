package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danh_muc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_danh_muc")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_danh_muc"))
})
public class DanhMuc extends BaseCodeNameEntity {

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
