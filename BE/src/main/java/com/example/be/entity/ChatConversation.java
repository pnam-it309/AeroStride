package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien_nhan")
    private NhanVien secondNhanVien; // Dùng cho chat nội bộ (người nhận)

    @Column(name = "session_id")
    private String sessionId; // Dùng cho khách vãng lai chưa đăng nhập

    @Builder.Default
    @Column(name = "is_accepted")
    private Boolean isAccepted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    @Builder.Default
    private ChatType type = ChatType.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private ChatStatus status = ChatStatus.PENDING;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("ngayTao ASC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    public enum ChatType {
        CUSTOMER, INTERNAL
    }

    public enum ChatStatus {
        PENDING, ACTIVE, CLOSED
    }
}
