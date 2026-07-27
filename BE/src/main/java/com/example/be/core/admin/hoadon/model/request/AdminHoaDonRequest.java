package com.example.be.core.admin.hoadon.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.OrderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminHoaDonRequest extends PageRequest {
    
    private String search;        // Tìm kiếm nhanh (thường là Mã hóa đơn)

    private String idKhachHang;   // Lọc chính xác theo ID khách hàng

    private String tenKhachHang;  // Lọc theo tên khách hàng

    private String soDienThoai;   // Lọc riêng theo số điện thoại (nếu cần)

    private Integer trangThai;    // Nhận giá trị 0, 1, 2, 3, 4 từ Select của FE

    private OrderType orderType;

    @Deprecated
    private String loaiDon;

    private String ngayTao;       // Nhận chuỗi "yyyy-MM-dd" từ input type="date" của FE

    private String tuNgay;

    private String denNgay;

    private Long tuNgayLong;

    private Long denNgayLong;

    // fields like page, size, sortDirection, sortBy are now inherited from PageRequest
}
