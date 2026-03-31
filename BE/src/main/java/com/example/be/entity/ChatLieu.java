package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_lieu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_chat_lieu")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_chat_lieu"))
})
public class ChatLieu extends BaseCodeNameEntity {

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
