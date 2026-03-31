package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "co_giay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_co_giay")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_co_giay"))
})
public class CoGiay extends BaseCodeNameEntity {

    @Column(name = "gia_tri_kich_thuoc")
    private String giaTriKichThuoc;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
