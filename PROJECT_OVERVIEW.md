# Tổng quan Dự án AeroStride

## 1. Mục tiêu hệ thống

AeroStride là hệ thống quản trị bán lẻ tập trung vào các nghiệp vụ:

- Quản lý sản phẩm và thuộc tính
- Bán hàng tại quầy
- Quản lý hóa đơn và thanh toán
- Đợt giảm giá và phiếu giảm giá
- Quản lý khách hàng, nhân viên
- Thống kê và quản lý file

Phần FE hiện thiên về giao diện quản trị nội bộ, đi qua layout `/main` và các module trong `FE/src/views/modules`.

## 2. Tech stack thực tế

### Backend

- Java 17
- Spring Boot 3.4.6
- Spring Data JPA
- Spring Security
- Redis
- WebSocket
- Flyway
- Spring Batch
- Cloudinary
- VNPay
- Thymeleaf Mail

### Frontend

- Vue 3
- Vite
- Vuetify 3
- Pinia
- Vue Router
- Axios
- Vitest

### Dữ liệu và hạ tầng

- MySQL là database runtime chính
- H2 có trong dependency runtime
- Redis dùng cho cache và các luồng cần state nhanh

## 3. Cấu trúc cấp cao

- `BE/`: backend Spring Boot
- `FE/`: frontend Vue/Vite
- `docs/`: tài liệu kỹ thuật chi tiết
- `docker/`: cấu hình chạy bằng container

## 4. Bản đồ backend

### Thư mục chính

- `BE/src/main/java/com/example/be/core/admin/*`: các module nghiệp vụ admin
- `BE/src/main/java/com/example/be/entity`: JPA entities
- `BE/src/main/java/com/example/be/repository`: repository dùng chung
- `BE/src/main/java/com/example/be/infrastructure/*`: security, config, constants, exceptions
- `BE/src/main/resources/db/migration`: migration Flyway

### Module admin đang có

- `banhang`
- `dotgiamgia`
- `hoadon`
- `khachhang`
- `nhanvien`
- `phieugiamgia`
- `sanpham`
- `thongke`
- `thuoctinh`

### Điểm vào quan trọng

- App entry: `BE/src/main/java/com/example/be/BeApplication.java`
- Route constants: `BE/src/main/java/com/example/be/infrastructure/constants/RoutesConstant.java`
- Security: `BE/src/main/java/com/example/be/infrastructure/security/SecurityConfig.java`
- Auth controller: `BE/src/main/java/com/example/be/infrastructure/security/controller/AuthController.java`
- Global exception handler: `BE/src/main/java/com/example/be/infrastructure/exceptions/GlobalExceptionHandler.java`

## 5. Bản đồ frontend

### Thư mục chính

- `FE/src/views/modules/*`: màn hình nghiệp vụ
- `FE/src/services/admin|product|sales|auth|core/*`: lớp gọi API
- `FE/src/router/*`: router và guards
- `FE/src/stores/*`: state dùng chung
- `FE/src/layouts/*`: layout hiển thị

### Module giao diện đang có

- `banhang`
- `san-pham`
- `khachhang`
- `hoa-don`
- `nhanvien`
- `dot-giam-gia`
- `phieu-giam-gia`
- `ThuocTinh.vue`
- `ThanhToan.vue`
- `ThongKe.vue`
- `QuanLyFile.vue`

### Điểm vào quan trọng

- FE entry: `FE/src/main.js`
- Main router: `FE/src/router/MainRoutes.js`
- Auth router: `FE/src/router/AuthRoutes.js`
- Guard: `FE/src/router/guards.js`
- Axios instance: `FE/src/services/apiService.js`

## 6. Luồng request điển hình

1. Người dùng thao tác trên màn hình trong `FE/src/views/modules/*`
2. FE gọi service tương ứng trong `FE/src/services/*`
3. `apiService.js` gắn token từ `sessionStorage`
4. Backend nhận request tại controller thuộc `core/admin/*/controller`
5. Service xử lý nghiệp vụ ở `core/admin/*/service/impl`
6. Repository/JPA truy cập DB
7. Response trả ngược lại FE; interceptor xử lý loader và lỗi 401/403/500

## 7. Map nhanh các module chính

### Bán hàng tại quầy

- FE: `FE/src/views/modules/banhang/BanHang.vue`
- BE: `BE/src/main/java/com/example/be/core/admin/banhang/*`

### Hóa đơn

- FE: `FE/src/views/modules/hoa-don/HoaDon.vue`, `HoaDonChiTiet.vue`
- BE: `BE/src/main/java/com/example/be/core/admin/hoadon/*`

### Sản phẩm

- FE: `FE/src/views/modules/san-pham/*`
- BE: `BE/src/main/java/com/example/be/core/admin/sanpham/*`

### Đợt giảm giá

- FE: `FE/src/views/modules/dot-giam-gia/*`
- BE: `BE/src/main/java/com/example/be/core/admin/dotgiamgia/*`

### Phiếu giảm giá

- FE: `FE/src/views/modules/phieu-giam-gia/*`
- BE: `BE/src/main/java/com/example/be/core/admin/phieugiamgia/*`

### Khách hàng và nhân viên

- FE: `FE/src/views/modules/khachhang/*`, `FE/src/views/modules/nhanvien/*`
- BE: `BE/src/main/java/com/example/be/core/admin/khachhang/*`, `nhanvien/*`

### Thuộc tính, thống kê, file

- FE: `ThuocTinh.vue`, `ThongKe.vue`, `QuanLyFile.vue`
- BE: `thuoctinh/*`, `thongke/*`, storage/security/infrastructure liên quan

## 8. Routes và điều hướng

### Backend

Các route gốc đang được gom ở `RoutesConstant.java`:

- `/api/v1/auth`
- `/api/v1/admin/ban-hang`
- `/api/v1/admin/hoa-don`
- `/api/v1/admin/san-pham`
- `/api/v1/admin/dot-giam-gia`
- `/api/v1/admin/phieu-giam-gia`
- `/api/v1/admin/khach-hang`
- `/api/v1/admin/nhan-vien`
- `/api/v1/admin/thuoc-tinh`

### Frontend

Router chính đi qua:

- `/auth/login`
- `/auth/register`
- `/main`
- `/ban-hang`
- `/san-pham`
- `/khach-hang`
- `/hoa-don`
- `/nhan-vien`
- `/dot-giam-gia`
- `/phieu-giam-gia`
- `/thuoc-tinh/:tab?`
- `/thanh-toan`
- `/quan-ly-file`
- `/thong-ke`

## 9. Cấu hình và môi trường

### Backend

- File cấu hình: `BE/src/main/resources/application.properties`
- File mẫu env: `BE/env/.env.example`
- Profile active mặc định theo `application.properties`: `dev`

Biến quan trọng:

- `SPRING_DATASOURCE_*`
- `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`
- `REDIS_HOST`, `REDIS_PORT`
- `ALLOWED_ORIGIN`
- `CLOUDINARY_*`
- `VNP_*`
- `APP_BOOTSTRAP_ADMIN_*`

### Frontend

- API base URL lấy từ `import.meta.env.VITE_API_URL`
- Nếu không có env, FE dùng mặc định `'/api/v1'`

## 10. Database và migration

Repo hiện có các migration:

- `V1__Initial_Schema.sql`
- `V2__Sample_Data.sql`

Khi thay đổi schema:

1. Tạo file migration mới trong `BE/src/main/resources/db/migration`
2. Không sửa trực tiếp file migration cũ nếu đã dùng ngoài môi trường local cá nhân

## 11. Auth và bảo mật

- JWT được gắn tại `FE/src/services/apiService.js`
- Token đang được lưu ở `sessionStorage`
- Guard route ở `FE/src/router/guards.js`
- Filter xác thực ở `BE/src/main/java/com/example/be/infrastructure/security/JwtAuthenticationFilter.java`

Khi sửa auth cần kiểm cùng lúc:

- FE guard
- FE logout/login flow
- BE security config
- Auth controller
- 401/403 handling

## 12. Test hiện có

### Backend

- `BE/src/test/java/com/example/be/BaseIntegrationTest.java`
- `BE/src/test/java/com/example/be/BeApplicationTests.java`
- Unit test cho nhiều admin service:
  - `AdminPhieuGiamGiaServiceImplTest`
  - `AdminDotGiamGiaServiceImplTest`
  - `AdminKhachHangServiceImplTest`
  - `AdminNhanVienServiceImplTest`
  - các service thuộc tính khác

### Frontend

- FE có `vitest` trong `package.json`
- Chưa thấy thư mục test nổi bật trong lần quét cấu trúc nhanh này

## 13. Cách lần theo một task

1. Xác định màn hình ở `FE/src/views/modules/*`
2. Tìm service FE gọi API
3. Mở controller BE tương ứng
4. Đọc service impl để hiểu rule
5. Kiểm repository/entity/migration liên quan
6. Nếu ảnh hưởng auth, kiểm `guards.js`, `apiService.js`, `SecurityConfig.java`

## 14. Cách lần theo bug

1. Xác định route và màn hình lỗi
2. Kiểm request ở FE service và payload gửi đi
3. Trace controller -> service -> repository
4. Kiểm response/error handler
5. Kiểm cấu hình env nếu lỗi phụ thuộc môi trường

## 15. Gợi ý đọc tiếp

- `DOCUMENTATION_INDEX.md`
- `DEVELOPMENT_WORKFLOW.md`
- `docs/backend_doc.md`
- `docs/frontend_doc.md`
