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
@Table(name = "xuat_xu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
        @AttributeOverride(name = "ma", column = @Column(name = "ma_xuat_xu")),
        @AttributeOverride(name = "ten", column = @Column(name = "ten_xuat_xu"))
})
public class XuatXu extends BaseCodeNameEntity {

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
