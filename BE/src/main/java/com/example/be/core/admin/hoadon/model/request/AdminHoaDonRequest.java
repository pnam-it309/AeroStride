package com.example.be.core.admin.hoadon.model.request;

import com.example.be.core.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminHoaDonRequest extends PageRequest {
    
    private String search;        // Tìm kiếm nhanh (thường là Mã hóa đơn)

    private String tenKhachHang;  // Lọc riêng theo tên khách hàng

    private String soDienThoai;   // Lọc riêng theo số điện thoại (nếu cần)

    private Integer trangThai;    // Nhận giá trị 0, 1, 2, 3, 4 từ Select của FE

    private String loaiDon;       // Nhận "TAI_QUAY" hoặc "ONLINE"

    private String ngayTao;       // Nhận chuỗi "yyyy-MM-dd" từ input type="date" của FE

    private String tuNgay;

    private String denNgay;

    private Long tuNgayLong;

    private Long denNgayLong;

    // fields like page, size, sortDirection, sortBy are now inherited from PageRequest
}
