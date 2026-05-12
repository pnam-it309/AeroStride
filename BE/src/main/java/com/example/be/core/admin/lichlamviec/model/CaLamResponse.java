package com.example.be.core.admin.lichlamviec.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CaLamResponse {
    private String id;
    private String tenCa;
    private String gioBatDau;
    private String gioKetThuc;
    private String moTa;
}
