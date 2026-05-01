package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mau_sac")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_mau_sac")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_mau_sac"))
})
public class MauSac extends BaseCodeNameEntity {

    @Column(name = "ma_mau_hex")
    private String maMauHex;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
