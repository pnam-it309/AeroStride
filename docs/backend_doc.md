# Kiến trúc Thư mục Backend (AeroStride BE)

Tài liệu này mô tả cấu trúc Backend dự án AeroStride sử dụng Spring Boot theo kiến trúc phân tầng (Layered Architecture).

## 1. Cấu trúc Tổng quan
```text
BE/src/main/java/com/example/be/
├── core/               # Lõi nghiệp vụ (Business Logic)
│   ├── admin/          # API dành cho quản trị viên
│   ├── staff/          # API dành cho nhân viên
│   ├── customer/       # API dành cho khách hàng (Client)
│   └── common/         # DTO/Cấu hình dùng chung cho các core
├── entity/             # Các JPA Entities (Ánh xạ Database)
├── infrastructure/     # Tầng hạ tầng (Cấu hình kỹ thuật)
│   ├── config/         # Spring Boot Config (CORS, Security)
│   ├── constants/      # Hằng số hệ thống (Routes, Enum)
│   ├── exceptions/     # Xử lý lỗi tập trung (Global Exception Handler)
│   ├── listener/       # Các listener (Entity Audit)
│   └── security/       # Cấu hình JWT, Authentication/Authorization
├── repository/         # Tầng lưu trữ (JPA Repositories)
└── utils/              # Các class tiện ích (String, DateTime)

BE/src/main/resources/
└── db/migration/       # Các file Flyway (Quản lý phiên bản DB)
```

## 2. Chi tiết các Thư mục chính

### `core/` (Cấu trúc module)
Mỗi module nhỏ (ví dụ: `sanpham`) sẽ được tổ chức theo:
- **controller**: Tiếp nhận request từ FE.
- **service**: Xử lý logic nghiệp vụ.
- **model/request**: Các DTO nhận dữ liệu từ FE.
- **model/response**: Các DTO trả dữ liệu về FE.

### `entity/`
- Chứa các lớp Java đại diện cho bảng trong Database.
- Sử dụng Hibernate/JPA và Lombok để tối giản code.

### `infrastructure/`
- **security/**: Chứa `JwtAuthenticationFilter` và `WebSecurityConfig` để bảo mật hệ thống.
- **constants/RoutesConstant.java**: Quản lý tập trung tất cả các API Endpoint.

---

## 3. Quy trình phát triển/Bảo trì
1. **Thêm API mới**: 
   - Định nghĩa route trong `RoutesConstant.java`.
   - Tạo Controller/Service trong gói `core/` tương ứng.
   - Thêm Repository nếu cần truy vấn mới.
2. **Thay đổi DB**: 
   - Không sửa trực tiếp trong DB.
   - Tạo file migration `.sql` mới trong `db/migration/`.
3. **Xử lý lỗi**: 
   - Ném ngoại lệ `RestApiException` để Global Exception Handler tự động trả về JSON chuẩn cho FE.
