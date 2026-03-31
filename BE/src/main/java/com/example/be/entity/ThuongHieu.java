package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "thuong_hieu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_thuong_hieu")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_thuong_hieu"))
})
public class ThuongHieu extends BaseCodeNameEntity {

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
