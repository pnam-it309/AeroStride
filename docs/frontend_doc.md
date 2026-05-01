# Kiến trúc Thư mục Frontend (AeroStride FE)

Tài liệu này mô tả cấu trúc thư mục và cách tổ chức các thành phần trong gói Frontend của dự án AeroStride.

## 1. Cấu trúc Tổng quan
```text
FE/src/
├── assets/          # Tài nguyên tĩnh (Hình ảnh, Font)
├── components/      # Các UI Components dùng chung
├── composable/      # Các Vue Composables (Logic tái sử dụng)
├── constants/       # Các hằng số (Routes, API Endpoints, Labels)
├── layouts/         # Các khung giao diện chính (AdminLayout, ShopLayout)
├── pages/           # Các trang/màn hình hoàn chỉnh
├── router/          # Cấu hình Vue Router (Điều hướng)
├── services/        # Các kết nối API (Axios services)
├── stores/          # Quản lý trạng thái (Pinia stores)
├── types/           # Định nghĩa các kiểu dữ liệu (nếu có)
└── utils/           # Các hàm công cụ (Format date, Validate)
```

## 2. Chi tiết các Thư mục chính

### `components/`
- **shared/admin/**: Chứa các component đặc thù cho giao diện Admin (Bảng dữ liệu, Bộ lọc chuẩn).
- **base/**: Các component cơ bản nhất (Nút bấm, Input, Modal tùy chỉnh).

### `pages/` (Cấu trúc phân tầng theo vai trò)
- **admin/**: Các trang quản lý (Sản phẩm, Hóa đơn, Nhân viên, Thuộc tính).
- **shop/**: Các trang cho khách hàng (Trang chủ, Chi tiết sản phẩm, Giỏ hàng).
- **auth/**: Các trang Đăng nhập, Đăng ký.
- **errors/**: Các trang lỗi (404, 500, 403).

### `services/`
- **api.js**: Cấu hình Axios instance (Base URL, Interceptors để xử lý Token).
- Các file `*Service.js`: Chứa các hàm gọi đến Backend (CRUD).

### `constants/`
- **NavigationConstants.js**: Định nghĩa menu sidebar (Icon, Label, Path).
- **RouteConstants.js**: Tập hợp tất cả các URL path của hệ thống.

---

## 3. Quy trình bảo trì (Maintenance)
1. **Thêm trang mới**: Tạo file `.vue` trong `pages/`, sau đó đăng ký vào `router/index.js`.
2. **Thay đổi API**: Cập nhật endpoint trong `services/` tương ứng.
3. **Thay đổi Giao diện**: Nếu là component dùng chung, hãy sửa trong `components/` để cập nhật toàn hệ thống.
