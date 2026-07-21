package com.example.be.core.admin.chat.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TinNhanResponse {
    private String id;
    
    @JsonProperty("conversationId")
    private String idCuocHoiThoai;
    
    @JsonProperty("sessionId")
    private String maPhien;
    
    @JsonProperty("staffId")
    private String idNhanVien; // Assigned staff for customer chat or sender for internal
    
    @JsonProperty("secondStaffId")
    private String idNhanVienNhan; // Receiver for internal chat
    
    @JsonProperty("sender")
    private String nguoiGui; // customer, staff, system
    
    @JsonProperty("text")
    private String noiDung;
    
    @JsonProperty("time")
    private String thoiGian;
}
