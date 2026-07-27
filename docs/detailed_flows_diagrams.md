# Sơ đồ Luồng Nghiệp vụ Chi tiết (Cấp độ Kỹ thuật / Database)

Tài liệu này trình bày các luồng nghiệp vụ **sâu sát với mã nguồn thực tế** (các bảng Entity, Logic Backend, State Machine) của dự án AeroStride. Nó dành cho Lập trình viên, Quản trị viên và người cần nắm vững logic kỹ thuật cốt lõi.

---

## 1. Luồng Bán Hàng Tại Quầy (POS) & Bán Hàng Online

Luồng xử lý đơn hàng tương tác trực tiếp với các thực thể: `HoaDon`, `HoaDonChiTiet`, `GiaoDichThanhToan`, và `ChiTietSanPham`.

```mermaid
sequenceDiagram
    autonumber
    actor KH as Khách (Online)
    actor NV as NV (POS)
    participant FE as Vue.js FE
    participant BE as Spring Boot (HoaDonController)
    participant DB_HD as HoaDon / HoaDonChiTiet
    participant DB_SP as ChiTietSanPham (Kho)

    rect rgb(232, 245, 233)
        Note over NV, DB_SP: 1.1 POS (Bán Tại Quầy - Trạng thái: TẠO MỚI)
        NV->>FE: Bấm "Tạo hóa đơn"
        FE->>BE: POST /api/v1/hoa-don
        BE->>DB_HD: INSERT HoaDon (orderType = TAI_QUAY)
        FE->>BE: Thêm sản phẩm (Quét Barcode)
        BE->>DB_SP: SELECT so_luong_ton
        DB_SP-->>BE: Kiểm tra tồn kho hợp lệ
        BE->>DB_HD: INSERT HoaDonChiTiet
        
        NV->>FE: Bấm "Thanh toán" (Tiền mặt/VNPay)
        FE->>BE: POST /api/v1/thanh-toan
        BE->>DB_HD: INSERT GiaoDichThanhToan (tienNguoiMua)
        BE->>DB_SP: UPDATE ChiTietSanPham (Trừ số lượng)
        BE->>DB_HD: UPDATE HoaDon (trangThai = HOAN_THANH)
    end

    rect rgb(225, 245, 254)
        Note over KH, DB_SP: 1.2 ONLINE (Đặt hàng Web - Trạng thái: CHỜ XÁC NHẬN)
        KH->>FE: Điền form checkout (DiaChi, PhieuGiamGia)
        FE->>BE: POST /api/v1/customer/hoa-don/dat-hang
        BE->>DB_SP: UPDATE ChiTietSanPham (Tạm giữ tồn kho)
        BE->>DB_HD: INSERT HoaDon (trangThai = CHO_XAC_NHAN, loaiDon = ONLINE)
        BE->>DB_HD: INSERT HoaDonChiTiet
        BE-->>FE: HTTP 200 (Success)
    end
```

---

## 2. Máy Trạng Thái Hóa Đơn & Vận Chuyển (Order State Machine)

Bám sát logic trong enum `OrderStatus.java`. Bất cứ sự thay đổi nào cũng ghi lại nhật ký vào bảng `LichSuTrangThaiHoaDon`.

```mermaid
stateDiagram-v2
    [*] --> 0_CHO_XAC_NHAN: Đặt hàng (Online)
    
    0_CHO_XAC_NHAN --> 1_XAC_NHAN: Admin / NV Xác nhận
    1_XAC_NHAN --> 2_CHO_GIAO: Xử lý kho / Đóng gói
    2_CHO_GIAO --> 3_DANG_GIAO: Giao cho ĐV Vận chuyển
    
    3_DANG_GIAO --> 4_HOAN_THANH: Giao thành công
    3_DANG_GIAO --> 6_HOAN_DON: Khách từ chối nhận (Giao thất bại)
    4_HOAN_THANH --> 6_HOAN_DON: Khách đổi/trả hàng sau khi nhận
    
    0_CHO_XAC_NHAN --> 5_DA_HUY: Khách/NV Hủy
    1_XAC_NHAN --> 5_DA_HUY: NV Hủy
    2_CHO_GIAO --> 5_DA_HUY: NV Hủy
    
    5_DA_HUY --> [*]: Hoàn tồn kho (Rollback)
    6_HOAN_DON --> [*]: Hoàn tồn kho & Hoàn tiền (phiHoanHang)
    4_HOAN_THANH --> [*]: Ghi nhận Doanh thu thực tế
```
*(Lưu ý: Hóa đơn POS (Tại quầy) đi thẳng tới trạng thái `4_HOAN_THANH`).*

---

## 3. Luồng Quản Lý Khách Hàng (Customer Lifecycle)

Logic bám sát vào bảng `KhachHang` và `DiaChi`. Khách hàng có thể được gán `PhieuGiamGiaCaNhan`.

```mermaid
flowchart TD
    classDef action fill:#bbdefb,stroke:#1976d2,stroke-width:2px;
    classDef state fill:#ffe082,stroke:#ffa000,stroke-width:2px;
    classDef db fill:#c8e6c9,stroke:#388e3c,stroke-width:2px;

    Start((Khởi tạo)) --> Register[Khách hàng Đăng ký (Web)]:::action
    Start --> POS_Add[Nhân viên thêm Khách (POS)]:::action
    
    Register --> BE_Auth[Tạo Account & Hash Password]:::action
    POS_Add --> BE_Auth
    
    BE_Auth --> DB_KH[(Bảng KhachHang)]:::db
    
    DB_KH --> Login[Đăng nhập sinh RefreshToken]:::action
    Login --> Profile[Xem Profile Hồ sơ]:::state
    
    Profile --> DiaChi[Quản lý Sổ Địa Chỉ]:::action
    DiaChi --> DB_DC[(Bảng DiaChi)]:::db
    DB_DC --> MặcĐịnh[Set 1 Địa chỉ Default để Checkout]
    
    Profile --> Voucher[Xem Ví Voucher]:::action
    Voucher --> DB_PGG[(Bảng PhieuGiamGiaCaNhan)]:::db
```

---

## 4. Kiến Trúc Sản Phẩm & Biến Thể (Product & SKU Architecture)

Ánh xạ logic từ các bảng `ThuongHieu`, `ChatLieu`, `KichThuoc`, `MauSac` cấu thành nên `SanPham` và cuối cùng là `ChiTietSanPham` (SKU thực tế đem bán).

```mermaid
flowchart TD
    classDef config fill:#e1bee7,stroke:#8e24aa,stroke-width:2px;
    classDef main fill:#ffcc80,stroke:#f57c00,stroke-width:2px;
    classDef sku fill:#a5d6a7,stroke:#2e7d32,stroke-width:2px;
    classDef asset fill:#b3e5fc,stroke:#0288d1,stroke-width:2px;

    T1(ThuongHieu):::config --> SP[SanPham (Root Product)]:::main
    T2(ChatLieu):::config --> SP
    T3(DeGiay / CoGiay):::config --> SP
    T4(MucDichChay):::config --> SP
    
    SP -->|+ KichThuoc + MauSac| CTSP[ChiTietSanPham (SKU Biến Thể)]:::sku
    
    CTSP --> Kho[so_luong_ton = int]
    CTSP --> Gia[gia_ban = BigDecimal]
    CTSP --> Barcode[Ma_vach / QR Code]
    
    CTSP --> Upload[Upload Ảnh]
    Upload --> Cloudinary((Cloudinary API)):::asset
    Cloudinary --> DB_Anh[(Bảng AnhChiTietSanPham)]:::asset
```

---

## 5. Luồng Đánh Giá Sản Phẩm (Reviews & Ratings)

Quá trình tương tác trên bảng `DanhGiaSanPham`.

```mermaid
sequenceDiagram
    actor KH as Khách (Online)
    participant BE as Spring Boot
    participant DB as Bảng DanhGiaSanPham
    actor Admin as Admin

    KH->>BE: Lấy danh sách SP đã mua (trangThai = HOAN_THANH)
    BE-->>KH: Trả về danh sách được phép Đánh giá
    
    KH->>BE: POST /api/v1/danh-gia (Sao, Nội dung, ID_CTSP)
    BE->>DB: INSERT DanhGiaSanPham (Trạng thái: Hiện)
    
    Admin->>BE: GET /api/v1/admin/danh-gia
    BE-->>Admin: Hiển thị List Đánh giá
    
    Admin->>BE: PUT /api/v1/admin/danh-gia/reply
    BE->>DB: Cập nhật phản hồi (Reply) từ Shop
    
    opt Nếu vi phạm ngôn từ
        Admin->>BE: Chuyển Trạng thái = Ẩn
        BE->>DB: UPDATE trangThai = INACTIVE
    end
```

---

## 6. Luồng Khuyến Mãi (Promotions - Campaigns vs Vouchers)

Chi tiết cách `DotGiamGia` (Campaigns) tác động trực tiếp lên giá sản phẩm, và `PhieuGiamGia` (Vouchers) tác động lên tổng hóa đơn lúc thanh toán.

```mermaid
flowchart TD
    classDef admin fill:#e1bee7,stroke:#8e24aa,stroke-width:2px;
    classDef logic fill:#fff8e1,stroke:#fbc02d,stroke-width:2px;
    classDef db fill:#c8e6c9,stroke:#388e3c,stroke-width:2px;
    classDef client fill:#e1f5fe,stroke:#03a9f4,stroke-width:2px;

    Admin((Quản trị viên)):::admin
    
    %% Nhánh Đợt Giảm Giá (Campaign)
    subgraph Campaign [1. Đợt Giảm Giá (Giảm trực tiếp Giá Sản Phẩm)]
        Admin -->|Tạo mới| DGG[DotGiamGia \n- loaiGiamGia %, VND\n- soTienGiam]:::db
        DGG -->|Áp dụng cho| CDGG[ChiTietDotGiamGia \nMapping]:::db
        CDGG -->|Liên kết tới| CTSP[ChiTietSanPham Biến thể]:::db
        CTSP -->|Logic Tính toán| GiaBanMoi[Giá Bán Mới = Giá Gốc - Giảm Giá]:::logic
    end
    
    %% Nhánh Phiếu Giảm Giá (Voucher)
    subgraph Voucher [2. Phiếu Giảm Giá (Giảm Tổng Hóa Đơn)]
        Admin -->|Tạo mới| PGG[PhieuGiamGia \n- hinhThuc Công khai/Cá nhân\n- phanTram/soTienGiam\n- donHangToiThieu, giamToiDa]:::db
        PGG --> CheckLoai{Hình Thức?}
        CheckLoai -->|Cá Nhân| PGG_CN[PhieuGiamGiaCaNhan \nGắn ID Khách Hàng]:::db
        CheckLoai -->|Công Khai| Public[Mã Code Public]:::logic
    end

    %% Thanh Toán (Checkout)
    KH((Khách hàng / POS)):::client -->|Checkout| HoaDon[(Hóa Đơn)]:::db
    GiaBanMoi -->|Thêm vào Giỏ| HoaDon
    Public -.->|Nhập Code| HoaDon
    PGG_CN -.->|Chọn từ Ví| HoaDon
    
    HoaDon --> Validation[Kiểm tra Hợp lệ: \n- Nằm trong Ngày BĐ/KT \n- Đạt Đơn Hàng Tối Thiểu \n- Còn Lượt soLuong]:::logic
    Validation -->|Hợp lệ| TongTien[Tính Tổng Tiền = SUM Giá Bán Mới - Tiền Giảm Voucher]:::logic
    Validation -->|Sai ĐK| Err[Báo Lỗi]:::logic
```

---

## 7. Luồng Ca Làm Việc & Giao Ca (Shift Handover)

Bám sát logic bảng `CaLam`, `LichLamViec`, và `GiaoCa` để quản trị dòng tiền vật lý tại cửa hàng.

```mermaid
sequenceDiagram
    actor Admin as Admin
    actor NV as Nhân viên
    participant DB_CL as CaLam / LichLamViec
    participant DB_GC as GiaoCa
    participant DB_HD as HoaDon

    Admin->>DB_CL: Tạo CaLam (vd: 8h-12h) & Xếp LichLamViec cho NV
    
    Note over NV, DB_HD: Bắt Đầu Ca
    NV->>DB_GC: Tạo GiaoCa (Nhập tiền ban đầu)
    DB_GC-->>NV: Trạng thái Ca: IN_PROGRESS
    
    loop Xảy ra trong Ca
        NV->>DB_HD: Tạo HoaDon POS
        DB_HD->>DB_GC: (Tự động liên kết id_giao_ca vào HoaDon)
    end
    
    Note over NV, DB_HD: Kết Thúc Ca (Bàn Giao)
    NV->>DB_GC: Yêu cầu Chốt Ca
    DB_GC->>DB_HD: SUM(tongTien) WHERE id_giao_ca = current
    DB_GC-->>NV: Hiển thị Báo cáo Doanh thu Hệ thống
    NV->>DB_GC: Nhập Số tiền thực tế đếm được
    DB_GC->>DB_GC: Tính độ lệch (Thừa/Thiếu), Đóng Ca
```

---

## 8. Luồng Trò Chuyện Trực Tuyến & AI (Live Chat & AI Bot)

Kết hợp bảng `CuocHoiThoai`, `TinNhan` và hệ thống WebSocket + AI Bot (`KienThucAi`, `TuDongNghiaAi`).

```mermaid
flowchart TD
    classDef client fill:#e1f5fe,stroke:#03a9f4,stroke-width:2px;
    classDef ws fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px;
    classDef db fill:#c8e6c9,stroke:#388e3c,stroke-width:2px;

    User((Khách Hàng)):::client -->|Send Message| WS[WebSocket Controller]:::ws
    
    WS --> DB_CHT[(CuocHoiThoai)]:::db
    WS --> DB_TN[(TinNhan)]:::db
    
    DB_TN --> AI_Check{Gọi AI Bot?}
    AI_Check -->|Có| AI_Logic[Truy vấn TuDongNghiaAi & KienThucAi]
    AI_Logic -->|Auto Reply| WS
    AI_Check -->|Không, cần NV| PushStaff[Push thông báo cho NV]
    
    PushStaff --> NV((Nhân Viên)):::client
    NV -->|Trả lời trực tiếp| WS
    WS --> User
```

---

## 9. Luồng Quản Lý Tệp File & Cloudinary (Assets)

Quá trình tải lên hình ảnh từ trình duyệt đi thẳng tới Cloudinary hoặc qua Backend proxy.

```mermaid
flowchart LR
    FE[Vue.js FE / Admin] -->|Multipart/form-data| BE_Upload[FileController / Upload Service]
    BE_Upload -->|Rest API| Cloudinary[(Cloudinary API)]
    Cloudinary -->|Secure URL (CDN)| BE_Upload
    BE_Upload --> DB[(MySQL)]
    DB --> |Trả URL về| FE
```

---

## 10. Luồng Thống Kê / Báo Cáo (Dashboard & Analytics)

Truy xuất dữ liệu tổng hợp dựa trên trạng thái `HOAN_THANH` của đơn hàng, kết xuất ra Dashboard biểu đồ cho Admin.

```mermaid
flowchart TD
    classDef db fill:#c8e6c9,stroke:#388e3c,stroke-width:2px;
    classDef logic fill:#fff8e1,stroke:#fbc02d,stroke-width:2px;
    
    DB_HD[(HoaDon)]:::db
    DB_HDCT[(HoaDonChiTiet)]:::db
    DB_KH[(KhachHang)]:::db
    
    DB_HD --> |WHERE trangThai=HOAN_THANH| ThongKe[ThongKeService]:::logic
    DB_HDCT --> |JOIN HoaDon| ThongKe
    DB_KH --> |Đếm Account Mới| ThongKe
    
    ThongKe --> |Data JSON (Date/Value)| FE_Chart[ThongKe.vue (ApexCharts / ChartJS)]
    
    FE_Chart --> C1[Biểu đồ Doanh Thu / Lợi Nhuận]
    FE_Chart --> C2[Biểu đồ SP Bán Chạy (Từ HDCT)]
    FE_Chart --> C3[Tỉ lệ Hủy/Thành công]
```
