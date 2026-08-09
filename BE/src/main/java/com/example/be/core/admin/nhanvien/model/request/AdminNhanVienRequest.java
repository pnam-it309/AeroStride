package com.example.be.core.admin.nhanvien.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class AdminNhanVienRequest extends PageRequest {

    @Size(max = 50, message = "Mã nhân viên không được vượt quá 50 ký tự")
    private String ma;

    @NotBlank(message = "Tên nhân viên không được để trống")
    @Size(max = 100, message = "Tên nhân viên không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[\\p{L}0-9\\s]+$", message = "Tên nhân viên không được chứa ký tự đặc biệt")
    private String ten;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @Size(max = 50, message = "Tên tài khoản không được vượt quá 50 ký tự")
    private String tenTaiKhoan;

    private String matKhau;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean gioiTinh;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ (10 chữ số, bắt đầu 03, 05, 07, 08, 09)")
    private String sdt;

    private LocalDate ngaySinh;
    private String hinhAnh;

    @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự")
    private String tinh;

    @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
    private String thanhPho;

    @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
    private String phuongXa;

    @Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự")
    private String diaChiChiTiet;

    @NotBlank(message = "Vui lòng chọn vai trò/phân quyền cho nhân viên")
    private String idPhanQuyen;

    // ── FILTER / TÌM KIẾM / LỌC / PHÂN TRANG ───────
    private String keyword;
    private String search;
    private TrangThai trangThai;

    public String getKeyword() {
        if (keyword != null && !keyword.isBlank()) {
            return keyword;
        }
        return search;
    }
}

