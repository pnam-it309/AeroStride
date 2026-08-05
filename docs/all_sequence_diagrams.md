# TỔNG HỢP TẤT CẢ MERMAID SEQUENCE DIAGRAMS - DỰ ÁN AEROSTRIDE

Tài liệu này chứa mã nguồn **Mermaid Sequence Diagram** của tất cả 10 phân hệ/màn hình chức năng trong dự án. Bạn có thể copy trực tiếp vào Draw.io (chọn *Insert > Advanced > Mermaid*) hoặc xem trực tiếp bằng Markdown Preview.

---

## 1. Quản Lý Sản Phẩm

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc thông tin sản phẩm
    Admin->>FE: Tạo một sản phẩm
    FE->>BE: Tạo một sản phẩm
    activate BE
    BE-->>FE: Sản phẩm đã được tạo
    deactivate BE
    FE->>BE: Tìm sản phẩm
    activate BE
    BE->>DB: Tìm sản phẩm
    activate DB
    DB-->>BE: Sản phẩm được tìm thấy
    deactivate DB
    BE-->>FE: Sản phẩm được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện cập nhật
    activate BE
    BE->>DB: Thực hiện cập nhật
    activate DB
    DB-->>BE: Cập nhật đã được hoàn tất
    deactivate DB
    BE-->>FE: Cập nhật đã được hoàn tất
    deactivate BE
```

---

## 2. Bán Hàng Tại Quầy (POS)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc thông tin sản phẩm
    Admin->>FE: Tạo một hóa đơn
    FE->>BE: Tạo một hóa đơn
    activate BE
    BE-->>FE: Hóa đơn đã được tạo
    deactivate BE
    FE->>BE: Tìm sản phẩm
    activate BE
    BE->>DB: Tìm sản phẩm
    activate DB
    DB-->>BE: Sản phẩm được tìm thấy
    deactivate DB
    BE-->>FE: Sản phẩm được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện thanh toán
    activate BE
    BE->>DB: Thực hiện thanh toán
    activate DB
    DB-->>BE: Thanh toán đã được thu
    deactivate DB
    BE-->>FE: Thanh toán đã được thu
    deactivate BE
```

---

## 3. Quản Lý Thuộc Tính Sản Phẩm

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc thuộc tính sản phẩm
    Admin->>FE: Tạo một thuộc tính
    FE->>BE: Tạo một thuộc tính
    activate BE
    BE-->>FE: Thuộc tính đã được tạo
    deactivate BE
    FE->>BE: Tìm thuộc tính
    activate BE
    BE->>DB: Tìm thuộc tính
    activate DB
    DB-->>BE: Thuộc tính được tìm thấy
    deactivate DB
    BE-->>FE: Thuộc tính được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện cập nhật
    activate BE
    BE->>DB: Thực hiện cập nhật
    activate DB
    DB-->>BE: Cập nhật đã được hoàn tất
    deactivate DB
    BE-->>FE: Cập nhật đã được hoàn tất
    deactivate BE
```

---

## 4. Quản Lý Hóa Đơn

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc thông tin hóa đơn
    Admin->>FE: Tạo một hóa đơn
    FE->>BE: Tạo một hóa đơn
    activate BE
    BE-->>FE: Hóa đơn đã được tạo
    deactivate BE
    FE->>BE: Tìm hóa đơn / Chi tiết
    activate BE
    BE->>DB: Tìm hóa đơn / Chi tiết
    activate DB
    DB-->>BE: Hóa đơn được tìm thấy
    deactivate DB
    BE-->>FE: Hóa đơn được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện cập nhật trạng thái
    activate BE
    BE->>DB: Thực hiện cập nhật trạng thái
    activate DB
    DB-->>BE: Cập nhật đã được hoàn tất
    deactivate DB
    BE-->>FE: Cập nhật đã được hoàn tất
    deactivate BE
```

---

## 5. Quản Lý Phiếu Giảm Giá

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc phiếu giảm giá
    Admin->>FE: Tạo phiếu giảm giá
    FE->>BE: Tạo phiếu giảm giá
    activate BE
    BE-->>FE: Phiếu giảm giá đã được tạo
    deactivate BE
    FE->>BE: Tìm phiếu giảm giá
    activate BE
    BE->>DB: Tìm phiếu giảm giá
    activate DB
    DB-->>BE: Phiếu giảm giá được tìm thấy
    deactivate DB
    BE-->>FE: Phiếu giảm giá được tìm thấy
    deactivate BE
    FE->>BE: Áp dụng phiếu giảm giá
    activate BE
    BE->>DB: Áp dụng phiếu giảm giá
    activate DB
    DB-->>BE: Phiếu giảm giá đã được áp dụng
    deactivate DB
    BE-->>FE: Phiếu giảm giá đã được áp dụng
    deactivate BE
```

---

## 6. Quản Lý Đợt Giảm Giá

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc đợt giảm giá
    Admin->>FE: Tạo đợt giảm giá
    FE->>BE: Tạo đợt giảm giá
    activate BE
    BE-->>FE: Đợt giảm giá đã được tạo
    deactivate BE
    FE->>BE: Tìm sản phẩm đợt giảm giá
    activate BE
    BE->>DB: Tìm sản phẩm đợt giảm giá
    activate DB
    DB-->>BE: Sản phẩm được tìm thấy
    deactivate DB
    BE-->>FE: Sản phẩm được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện kích hoạt
    activate BE
    BE->>DB: Thực hiện kích hoạt
    activate DB
    DB-->>BE: Kích hoạt đã được hoàn tất
    deactivate DB
    BE-->>FE: Kích hoạt đã được hoàn tất
    deactivate BE
```

---

## 7. Quản Lý Nhân Viên

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc thông tin nhân viên
    Admin->>FE: Tạo tài khoản nhân viên
    FE->>BE: Tạo tài khoản nhân viên
    activate BE
    BE-->>FE: Tài khoản nhân viên đã được tạo
    deactivate BE
    FE->>BE: Tìm nhân viên
    activate BE
    BE->>DB: Tìm nhân viên
    activate DB
    DB-->>BE: Nhân viên được tìm thấy
    deactivate DB
    BE-->>FE: Nhân viên được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện cập nhật
    activate BE
    BE->>DB: Thực hiện cập nhật
    activate DB
    DB-->>BE: Cập nhật đã được hoàn tất
    deactivate DB
    BE-->>FE: Cập nhật đã được hoàn tất
    deactivate BE
```

---

## 8. Quản Lý Khách Hàng

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc thông tin khách hàng
    Admin->>FE: Tạo tài khoản khách hàng
    FE->>BE: Tạo tài khoản khách hàng
    activate BE
    BE-->>FE: Khách hàng đã được tạo
    deactivate BE
    FE->>BE: Tìm khách hàng
    activate BE
    BE->>DB: Tìm khách hàng
    activate DB
    DB-->>BE: Khách hàng được tìm thấy
    deactivate DB
    BE-->>FE: Khách hàng được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện cập nhật
    activate BE
    BE->>DB: Thực hiện cập nhật
    activate DB
    DB-->>BE: Cập nhật đã được hoàn tất
    deactivate DB
    BE-->>FE: Cập nhật đã được hoàn tất
    deactivate BE
```

---

## 9. Thống Kê

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc thông tin thống kê
    Admin->>FE: Tạo báo cáo thống kê
    FE->>BE: Tạo báo cáo thống kê
    activate BE
    BE-->>FE: Báo cáo đã được tạo
    deactivate BE
    FE->>BE: Tìm dữ liệu thống kê
    activate BE
    BE->>DB: Tìm dữ liệu thống kê
    activate DB
    DB-->>BE: Dữ liệu được tìm thấy
    deactivate DB
    BE-->>FE: Dữ liệu được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện xuất báo cáo
    activate BE
    BE->>DB: Thực hiện xuất báo cáo
    activate DB
    DB-->>BE: Báo cáo đã được xuất
    deactivate DB
    BE-->>FE: Báo cáo đã được xuất
    deactivate BE
```

---

## 10. Đặc Tả Chức Năng

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Admin->>FE: Xem/lọc chức năng hệ thống
    Admin->>FE: Khởi tạo cấu hình chức năng
    FE->>BE: Khởi tạo cấu hình chức năng
    activate BE
    BE-->>FE: Chức năng đã được khởi tạo
    deactivate BE
    FE->>BE: Tìm quyền chức năng
    activate BE
    BE->>DB: Tìm quyền chức năng
    activate DB
    DB-->>BE: Quyền được tìm thấy
    deactivate DB
    BE-->>FE: Quyền được tìm thấy
    deactivate BE
    FE->>BE: Thực hiện phân quyền
    activate BE
    BE->>DB: Thực hiện phân quyền
    activate DB
    DB-->>BE: Phân quyền đã hoàn tất
    deactivate DB
    BE-->>FE: Phân quyền đã hoàn tất
    deactivate BE
```
