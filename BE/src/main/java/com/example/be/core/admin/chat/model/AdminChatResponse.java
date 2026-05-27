package com.example.be.core.admin.chat.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminChatResponse {
    private String id;
    @JsonProperty("name")
    private String ten;
    @JsonProperty("lastMsg")
    private String tinNhanCuoi;
    @JsonProperty("time")
    private String thoiGian;
    @JsonProperty("unread")
    private Integer chuaDoc;
    @JsonProperty("avatar")
    private String anhDaiDien;
    
    @JsonProperty("isAccepted")
    private Boolean daChapNhan;
    
    @JsonProperty("type")
    private String loaiHoiThoai; // CUSTOMER, INTERNAL
    
    @JsonProperty("status")
    private String trangThaiHoiThoai; // PENDING, ACTIVE, CLOSED
}
