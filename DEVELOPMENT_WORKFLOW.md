# Quy trình Làm việc AeroStride

## 1. Trước khi bắt đầu task

1. Đọc `PROJECT_OVERVIEW.md` để xác định module.
2. Xác nhận luồng ảnh hưởng:
   - FE view nào
   - FE service nào
   - BE controller/service nào
   - DB/migration nào
3. Kiểm worktree có thay đổi local sẵn hay không trước khi sửa.

## 2. Cách lần từ UI tới source

### Nếu task bắt đầu từ màn hình FE

1. Mở file trong `FE/src/views/modules/*`
2. Tìm lời gọi trong `FE/src/services/*`
3. Xác định route API
4. Mở controller backend tương ứng
5. Đi tiếp vào service impl và repository

### Nếu task bắt đầu từ API/backend

1. Mở controller
2. Xác định request/response model
3. Đọc service impl
4. Tìm FE service hoặc màn hình nào đang dùng API đó

## 3. Chạy local

### Backend

```powershell
cd BE
.\gradlew.bat test
.\gradlew.bat bootRun
```

### Frontend

```powershell
cd FE
npm install
npm run dev
npm run test
```

## 4. Khi sửa code

- Giữ đúng pattern thư mục đang có của module.
- Nếu thêm API mới, cập nhật route constants ở backend nếu cần.
- Nếu thay đổi schema, tạo migration mới thay vì chỉnh dữ liệu thủ công.
- Nếu thay đổi payload, kiểm cả FE service lẫn BE DTO.
- Nếu thay đổi auth/quyền, kiểm đồng thời guard, interceptor và security config.

## 5. Checklist trước khi commit

- Đã chạy test liên quan hoặc kiểm manual flow chính
- Đã kiểm lại request/response contract FE-BE
- Đã rà ảnh hưởng đến module liên đới
- Đã đọc nhanh `SECURITY_CHECKLIST.md` nếu đổi auth, upload, dữ liệu nhạy cảm
- Đã cập nhật docs nếu thay đổi kiến trúc hoặc rule nghiệp vụ

## 6. Checklist debug nhanh

- Lỗi nằm ở FE route, FE service hay BE API?
- Có phải lỗi env local không?
- Có khác biệt giữa dữ liệu mẫu và dữ liệu thật không?
- Có dính 401/403 do token, role hoặc guard không?
- Có migration mới nhưng DB local chưa apply không?

## 7. Khi thay đổi lớn

Hãy cập nhật `MODIFICATION_LOG.md` nếu thay đổi thuộc một trong các nhóm sau:

- Refactor nhiều module
- Đổi luồng nghiệp vụ chính
- Đổi contract API được nhiều màn hình sử dụng
- Đổi cấu hình nền tảng hoặc cơ chế bảo mật

## 8. Mẹo lần nhanh file cần sửa

- Sửa bán hàng: `banhang` + `dichVuDonHang.js` hoặc `dichVuThanhToan.js`
- Sửa sản phẩm: `san-pham` + `dichVuSanPham.js`
- Sửa khách hàng: `khachhang` + `dichVuKhachHang.js`
- Sửa hóa đơn: `hoa-don` + `dichVuHoaDon.js`
- Sửa nhân viên: `nhanvien` + `dichVuNhanVien.js`
- Sửa giảm giá: `dot-giam-gia`, `phieu-giam-gia`

## 9. Sau khi xong task

1. Tự kiểm lại các flow chính bị ảnh hưởng
2. Kiểm file config có vô tình thay đổi ngoài ý muốn không
3. Chuẩn bị ghi chú ngắn:
   - Đã sửa gì
   - Kiểm thử gì
   - Còn rủi ro nào
