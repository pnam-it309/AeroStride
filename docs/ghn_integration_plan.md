# Kế hoạch Tích hợp Tính Phí Vận Chuyển và Phí Hoàn Hàng (GHN)

Tính năng Bán hàng và Quản lý Đơn hàng cần tích hợp tính toán phí vận chuyển (khi tạo đơn/giao hàng) và phí hoàn hàng (khi có yêu cầu trả lại hàng) qua dịch vụ của Giao Hàng Nhanh (GHN).

## User Review Required

> [!IMPORTANT]
> **Xác nhận về cấu trúc Database**: Hiện tại bảng `HoaDon` đã có cột `phiVanChuyen` (Phí giao hàng). Với trường hợp hoàn hàng, mình sẽ thêm cột `phiHoanHang` để lưu lại phí vận chuyển chiều về. Hoặc bạn muốn trừ trực tiếp vào tổng tiền hoàn/cộng dồn vào `phiVanChuyen`? (Khuyến nghị: Thêm cột `phiHoanHang` để dễ đối soát).

> [!WARNING]
> **Tài khoản GHN**: Để gọi API GHN (tính phí), chúng ta sẽ cần thêm cấu hình `GHN_TOKEN` và `GHN_SHOP_ID` vào file `application.properties` ở phía Backend. Bạn đã có sẵn token của môi trường Test/Production chưa? 

## Open Questions

1. **Phạm vi hiển thị phí ship**: Trong chức năng Bán hàng (POS - tại quầy) thường không có phí ship, phí ship sẽ áp dụng cho **Bán hàng giao đi / Online**. Frontend hiện tại đã có dropdown chọn Tỉnh/Thành, Quận/Huyện, Phường/Xã lấy từ GHN chưa?
2. **Quy tắc Phí Hoàn Hàng**: Phí hoàn hàng có do cửa hàng chịu 100% hay trừ vào tiền hoàn của khách?

## Proposed Changes

---

### Backend (BE)

Sẽ thực hiện cập nhật BE để giao tiếp với GHN một cách an toàn và xử lý tính phí hoàn hàng khi đơn hàng đổi trạng thái.

#### [MODIFY] `src/main/resources/application.properties`
- Thêm cấu hình môi trường cho GHN:
  ```properties
  ghn.api.url=https://dev-online-gateway.ghn.vn/shiip/public-api
  ghn.api.token=${GHN_TOKEN}
  ghn.api.shopId=${GHN_SHOP_ID}
  ```

#### [MODIFY] `src/main/java/com/example/be/entity/HoaDon.java`
- Thêm thuộc tính lưu phí hoàn hàng:
  ```java
  @Column(name = "phi_hoan_hang")
  private BigDecimal phiHoanHang;
  ```

#### [NEW] `src/main/java/com/example/be/infrastructure/client/GhnClient.java`
- Tạo class Client dùng `RestTemplate` hoặc `WebClient` để gọi các API của GHN:
  - `POST /v2/shipping-order/fee` (Tính phí vận chuyển)
  - Hoặc API riêng để tính phí hoàn hàng (nếu có yêu cầu từ nghiệp vụ) - Mặc định sẽ gọi lại API tính phí với chiều ngược lại hoặc dựa trên webhook/response lúc hoàn đơn của GHN.

#### [NEW] `src/main/java/com/example/be/core/admin/hoadon/controller/GhnController.java`
- Thêm các endpoint (Proxy) cho Frontend gọi lấy danh sách Tỉnh/Huyện/Xã và Tính phí ship (ẩn token của GHN trên Server).
  - `GET /api/admin/ghn/fee?to_district_id=...&to_ward_code=...&weight=...`

#### [MODIFY] `src/main/java/com/example/be/core/admin/hoadon/service/impl/AdminHoaDonServiceImpl.java`
- Cập nhật logic: Khi chuyển trạng thái đơn hàng sang `HOAN_DON` (Hoàn đơn) / `DA_HUY` do khách trả, thực hiện tính toán / gán phí hoàn hàng vào cột `phiHoanHang` và lưu lại thông tin.

---

### Frontend (FE)

Sẽ bổ sung giao diện hiển thị phí trên cả 2 màn hình Tạo đơn và Xử lý trả hàng.

#### [MODIFY] `Màn hình Bán Hàng (Tạo đơn giao đi)`
- Khi người dùng (Admin) chọn địa chỉ Tỉnh/Huyện/Xã của khách, FE sẽ gọi endpoint `/api/admin/ghn/fee`.
- Cập nhật biến `Phí vận chuyển` trên UI theo dữ liệu trả về từ API GHN và cộng vào `Tổng tiền`.

#### [MODIFY] `Màn hình Chi tiết Đơn Hàng (Xử lý Hoàn/Trả)`
- Trong modal xác nhận Hoàn hàng, hiển thị mục **Phí hoàn hàng (dự kiến)**.
- Gửi thông tin phí này lên Backend để lưu trữ khi xác nhận hoàn đơn.

---

## Verification Plan

### Automated Tests
- Kiểm tra việc gọi API GHN bằng công cụ Test (Unit Test / API Client).

### Manual Verification
1. Truy cập chức năng **Bán hàng** ở FE -> Chọn giao hàng tận nơi -> Chọn địa chỉ.
2. Kiểm tra log/Network xem có gọi API `fee` và Phí vận chuyển trên màn hình có tự động thay đổi đúng với GHN không.
3. Truy cập **Quản lý đơn hàng** -> Thực hiện thao tác **Hoàn đơn**.
4. Kiểm tra xem Phí hoàn hàng có được lưu vào Database và hiển thị đúng không.
