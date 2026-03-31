# AeroStride

## 1. Tổng quan
AeroStride là một nền tảng thương mại điện tử (E-commerce) chuyên biệt dành cho các sản phẩm liên quan đến hàng không và mô hình máy bay. Hệ thống cung cấp trải nghiệm mua sắm toàn diện từ tìm kiếm, đặt hàng, thanh toán trực tuyến đến quản lý đơn hàng và chăm sóc khách hàng.

## 2. Kiến trúc hệ thống

### 2.1. Công nghệ sử dụng
- **Frontend**: Vue 3, Pinia, Tailwind CSS, Axios
- **Backend**: Spring Boot, MySQL, Redis, WebSocket, VNPAY
- **Deployment**: Docker, Docker Compose

### 2.2. Cấu trúc dự án
- **FE (Frontend)**: Ứng dụng web SPA (Single Page Application) được đóng gói thành file tĩnh.
- **BE (Backend)**: Ứng dụng Java Spring Boot xử lý logic nghiệp vụ và API.
- **Docker**: Sử dụng Docker Compose để quản lý và khởi động toàn bộ hệ thống (Database, Redis, Backend, Frontend).

## 3. Tính năng chính

### 3.1. Quản lý sản phẩm
- **Danh mục đa cấp**: Phân loại sản phẩm theo ngành hàng (Mô hình, Phụ kiện, Dụng cụ).
- **Tìm kiếm nâng cao**: Tìm kiếm theo tên, mô tả, giá, thương hiệu.
- **Chi tiết sản phẩm**: Hiển thị hình ảnh, video, thông số kỹ thuật, đánh giá.

### 3.2. Giỏ hàng & Thanh toán
- **Quản lý giỏ hàng**: Thêm, xóa, cập nhật số lượng sản phẩm.
- **Thanh toán trực tuyến**: Tích hợp cổng thanh toán VNPAY (Sandbox).
- **Quản lý đơn hàng**: Theo dõi trạng thái đơn hàng (Chờ xử lý, Đang giao, Hoàn thành, Hủy).
- **Lịch sử giao dịch**: Xem lại các đơn hàng đã thực hiện.

### 3.3. Xác thực & Bảo mật
- **Đăng ký/Đăng nhập**: Sử dụng JWT (JSON Web Token) để xác thực.
- **Quản lý vai trò**: Phân quyền giữa Khách hàng (USER) và Quản trị viên (ADMIN).
- **Bảo mật API**: Sử dụng HTTPS và mã hóa mật khẩu.

### 3.4. Thông báo thời gian thực
- **WebSocket**: Kết nối real-time giữa client và server.
- **Thông báo đơn hàng**: Cập nhật trạng thái đơn hàng ngay lập tức.
- **Thông báo hệ thống**: Broadcast thông báo đến tất cả người dùng đang online.

## 4. Hướng dẫn triển khai (Docker)

### 4.1. Yêu cầu
- Đã cài đặt Docker và Docker Compose.

### 4.2. Các lệnh
1. **Khởi động hệ thống**:
   ```bash
   docker-compose up --build
   ```

2. **Dừng hệ thống**:
   ```bash
   docker-compose down
   ```

### 4.3. Truy cập ứng dụng
- **Frontend**: `http://localhost:5173`
- **Backend API**: `http://localhost:8080`

## 5. Cấu hình môi trường

### 5.1. Backend (`.env.dev`)
- **Database**: MySQL (Port 3306)
- **Redis**: Port 6379
- **VNPAY**: Sử dụng chế độ Sandbox.
- **CORS**: Cho phép truy cập từ `http://localhost:5173`.

### 5.2. Frontend (`.env`)
- **API URL**: Trỏ về `http://localhost:8080`.
- **Base URL**: `/AeroStride/` (Để deploy trên các nền tảng có sub-path như Vercel/GitHub Pages).
