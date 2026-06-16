package com.example.be.core.admin.lichlamviec.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LichLamViecRequest {
    private List<String> nhanVien;
    private List<String> ca;
    private String ngay;
    private String trangThai;
}
