package com.example.be.core.customer.chat.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTinNhanResponse {
    private String id;
    
    @JsonProperty("conversationId")
    private String idCuocHoiThoai;
    
    @JsonProperty("sessionId")
    private String maPhien;
    
    @JsonProperty("sender")
    private String nguoiGui;
    
    @JsonProperty("text")
    private String noiDung;
    
    @JsonProperty("time")
    private String thoiGian;
    
    @JsonProperty("staffId")
    private String idNhanVien;
    
    @JsonProperty("secondStaffId")
    private String idNhanVienNhan;
}
