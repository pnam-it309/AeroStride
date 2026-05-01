package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "de_giay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_de_giay")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_de_giay"))
})
public class DeGiay extends BaseCodeNameEntity {

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
