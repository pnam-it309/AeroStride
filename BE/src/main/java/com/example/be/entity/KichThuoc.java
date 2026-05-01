package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kich_thuoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_kich_thuoc")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_kich_thuoc"))
})
public class KichThuoc extends BaseCodeNameEntity {

    @Column(name = "gia_tri_kich_thuoc")
    private String giaTriKichThuoc;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
