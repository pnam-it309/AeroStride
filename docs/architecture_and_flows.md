# Tài liệu Cấu trúc và Luồng nghiệp vụ Dự án AeroStride

Tài liệu này mô tả tổng quan về cấu trúc thư mục của toàn bộ dự án, cũng như chi tiết các luồng nghiệp vụ chính bao gồm Bán hàng, Khách hàng và Sản phẩm.

## 1. Cấu trúc toàn bộ dự án

Dự án được thiết kế theo mô hình Client-Server, bao gồm các thành phần chính:
- **BE (Backend):** Xây dựng bằng Java Spring Boot, cung cấp các API RESTful.
- **FE (Frontend):** Xây dựng bằng Vue.js, giao diện dành cho Admin/Nhân viên quản lý và bán hàng (POS).
- **Mobile (tùy chọn/tương lai):** Ứng dụng di động nếu có.
- **Cơ sở dữ liệu:** Sử dụng CSDL quan hệ (MySQL/PostgreSQL) thông qua Spring Data JPA.

### 1.1. Cấu trúc Backend (`/BE/src/main/java/com/example/be`)
- `controller/`: Chứa các REST Controller tiếp nhận HTTP Request từ Client (vd: `BanHangController`, `SanPhamController`, `KhachHangController`).
- `service/` & `service/impl/`: Chứa logic nghiệp vụ (Business Logic) của hệ thống.
- `repository/`: Các Interface mở rộng từ JpaRepository để thao tác trực tiếp với cơ sở dữ liệu.
- `entity/`: Các lớp mô hình thực thể ánh xạ với các bảng trong CSDL (vd: `HoaDon`, `KhachHang`, `SanPham`, `ChiTietSanPham`).
- `dto/`: Data Transfer Object - đối tượng truyền tải dữ liệu giữa Client và Server (chứa request/response objects).
- `infrastructure/`: Các cấu hình hệ thống bao gồm:
  - `security/`: Cấu hình xác thực và phân quyền (Spring Security, JWT Token).
  - `exception/`: Xử lý ngoại lệ tập trung (Global Exception Handler).
  - `storage/`: Tích hợp dịch vụ lưu trữ file (Cloudinary, Local).
  - `websocket/`: Cấu hình giao tiếp thời gian thực cho thông báo.

### 1.2. Cấu trúc Frontend (`/FE/src`)
- `views/modules/`: Chứa các trang giao diện chức năng chính của hệ thống.
  - `banhang/`: Giao diện Bán hàng tại quầy (POS), giỏ hàng, thanh toán.
  - `san-pham/`, `bien-the-san-pham/`, `thuoctinh/`: Quản lý danh mục sản phẩm, biến thể và thuộc tính (màu sắc, kích cỡ,...).
  - `khachhang/`: Quản lý thông tin khách hàng và địa chỉ.
  - `hoa-don/`: Quản lý danh sách và trạng thái hóa đơn.
- `services/`: Các file gọi API giao tiếp với Backend thông qua Axios.
- `stores/`: Quản lý State toàn cục (dùng Pinia hoặc Vuex).
- `components/`: Các UI Component có thể tái sử dụng.
- `router/`: Cấu hình định tuyến (Vue Router).

---

## 2. Luồng nghiệp vụ Bán Hàng (Sales Flow)

Hệ thống hỗ trợ 2 hình thức bán hàng chính: Bán hàng tại quầy (POS - Offline) và Bán hàng trực tuyến (Online).

### 2.1. Bán hàng tại quầy (POS)
1. **Khởi tạo hóa đơn:** Nhân viên tạo một "Hóa đơn chờ" mới tại quầy. Hệ thống hỗ trợ mở nhiều tab hóa đơn chờ cùng lúc (tối đa thường là 5 tab).
2. **Thêm sản phẩm:** 
   - Nhân viên tìm kiếm sản phẩm theo mã, tên hoặc quét mã vạch (Barcode/QR).
   - Chọn biến thể sản phẩm (Màu sắc, Kích cỡ) và số lượng. Hệ thống sẽ kiểm tra tồn kho (`soLuongTon`) trước khi thêm vào giỏ.
3. **Chọn khách hàng:** 
   - Tìm kiếm khách hàng có sẵn qua SĐT/Tên, hoặc thêm mới khách hàng ngay tại quầy nếu là khách mới.
   - Nếu không chọn, hệ thống mặc định là "Khách lẻ".
4. **Áp dụng khuyến mãi/Voucher:** Nhân viên có thể chọn các mã giảm giá áp dụng cho tổng hóa đơn.
5. **Thanh toán:**
   - Hệ thống tự động tính tổng tiền: `Tiền hàng - Giảm giá + Phụ phí (nếu có)`.
   - Chọn phương thức thanh toán: Tiền mặt (Cash) hoặc Chuyển khoản (VNPay/QR).
   - Nếu khách đưa tiền mặt, hệ thống tính toán tiền thối lại.
6. **Hoàn tất:** Xác nhận thanh toán, hệ thống trừ tồn kho, cập nhật trạng thái hóa đơn thành "Hoàn thành" và cho phép in biên lai (Invoice).

### 2.2. Bán hàng trực tuyến (Online - Xử lý đơn)
1. Khách hàng đặt hàng từ nền tảng trực tuyến.
2. Đơn hàng đẩy về hệ thống Admin với trạng thái "Chờ xác nhận".
3. Nhân viên tiến hành: Xác nhận đơn -> Chờ giao hàng -> Đang giao hàng -> Hoàn thành.
4. Giao dịch trực tuyến có tích hợp phí vận chuyển và quản lý địa chỉ nhận hàng riêng biệt.

---

## 3. Luồng nghiệp vụ Khách Hàng (Customer Flow)

Quản lý toàn bộ thông tin và vòng đời của khách hàng.

1. **Đăng ký / Thêm mới:**
   - Khách hàng tự đăng ký trên hệ thống Online, hoặc Nhân viên tạo mới thông qua giao diện POS/Admin.
   - Cần thu thập thông tin cơ bản: Tên, Số điện thoại (bắt buộc/để tra cứu), Email, Giới tính, Ngày sinh.
2. **Quản lý địa chỉ:**
   - Mỗi khách hàng có thể có nhiều địa chỉ nhận hàng (để phục vụ cho mua online). Có một địa chỉ được đánh dấu là "Mặc định".
3. **Tra cứu & Lịch sử mua hàng:**
   - Khi khách hàng đến quầy, nhân viên dùng SĐT để tra cứu.
   - Quản trị viên có thể xem lịch sử các đơn hàng khách đã mua, tổng chi tiêu để phục vụ cho các chiến dịch CSKH hoặc phân hạng thành viên.
4. **Bảo mật:** Thông tin khách hàng được quản lý bởi `KhachHangRepository` và các API tại `KhachHangController`.

---

## 4. Luồng nghiệp vụ Sản Phẩm (Product Flow)

Sản phẩm trong hệ thống được thiết kế theo mô hình **Sản phẩm cha (Product) -> Biến thể chi tiết (Product Variant)**.

1. **Khởi tạo Thuộc tính:**
   - Trước khi tạo sản phẩm, cần có các thuộc tính cấu thành: Thương hiệu, Danh mục (Mục đích chạy), Chất liệu, Đế giày, Cổ giày, Màu sắc, Kích cỡ.
2. **Tạo Sản phẩm gốc:**
   - Tạo thông tin chung (Tên sản phẩm, Mô tả, Thương hiệu, Chất liệu,...).
3. **Tạo Biến thể (Chi tiết sản phẩm):**
   - Từ sản phẩm gốc, hệ thống kết hợp với Màu sắc + Kích cỡ để sinh ra các SKU (Biến thể) riêng biệt.
   - Mỗi biến thể có: Giá bán riêng, Số lượng tồn kho độc lập, Hình ảnh riêng và Mã vạch (Barcode) dùng để quét khi bán hàng.
4. **Quản lý tồn kho:**
   - Khi nhập hàng: Tồn kho biến thể tăng lên.
   - Khi bán hàng (POS hoặc Online thành công): Tồn kho biến thể giảm xuống.
   - Nếu tồn kho = 0, hệ thống cảnh báo "Hết hàng" và không cho phép thêm vào giỏ hàng.
5. **Khuyến mãi sản phẩm (Đợt giảm giá):**
   - Quản trị viên có thể tạo các đợt giảm giá (ví dụ: Black Friday) và áp dụng % giảm giá trực tiếp lên giá bán của các biến thể sản phẩm trong một khoảng thời gian nhất định.
