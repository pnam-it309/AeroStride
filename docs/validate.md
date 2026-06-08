# Tài liệu Validation Form - Dự án AeroStride

Tài liệu này tổng hợp toàn bộ các quy tắc xác thực (validation) đối với các trường nhập liệu (inputs) trong hai module quan trọng là **Quản lý Sản phẩm**, **Quản lý Thuộc tính**, và **Bán hàng (POS)**.

---

## 1. Màn hình Quản lý Sản phẩm & Thuộc tính (`SanPhamForm.vue`, `ThuocTinhForm.vue`...)

Màn hình thêm mới và cập nhật sản phẩm / thuộc tính yêu cầu xác thực chặt chẽ thông tin để đảm bảo tính đồng nhất của dữ liệu.

### 1.1. Thông tin chung của sản phẩm (Inputs)
Tất cả các trường thông tin cơ bản của sản phẩm đều là **Bắt buộc nhập/chọn** (Required).

| Tên trường (Input) | Loại Input | Quy tắc Validate (Rules) |
| --- | --- | --- |
| **Tên sản phẩm** | Text Field | Bắt buộc nhập (`required`). **Tối đa 250 ký tự**. |
| **Mô tả chi tiết** | Text Area / Editor | Không giới hạn ký tự. Tùy chọn. |
| **Thương hiệu** | Combobox | Bắt buộc chọn từ danh sách hoặc nhập chữ để thêm mới. |
| **Danh mục** | Combobox | Bắt buộc chọn từ danh sách hoặc nhập chữ để thêm mới. |
| **Xuất xứ** | Combobox | Bắt buộc chọn từ danh sách hoặc nhập chữ để thêm mới. |
| **Chất liệu** | Combobox | Bắt buộc chọn từ danh sách hoặc nhập chữ để thêm mới. |
| **Đối tượng** | Select | Bắt buộc chọn (Nam, Nữ, Unisex). |
| **Mục đích** | Combobox | Bắt buộc chọn từ danh sách hoặc nhập chữ để thêm mới. |
| **Loại đế** | Combobox | Bắt buộc chọn từ danh sách hoặc nhập chữ để thêm mới. |
| **Loại cổ giày** | Combobox | Bắt buộc chọn từ danh sách hoặc nhập chữ để thêm mới. |

### 1.2. Màn hình Quản lý Thuộc tính (Màu sắc, Kích thước, Danh mục...)
Khi người dùng Thêm/Sửa nhanh hoặc vào Quản lý Thuộc tính:

| Tên trường | Loại Input | Quy tắc Validate (Rules) |
| --- | --- | --- |
| **Tên thuộc tính** | Text Field | Bắt buộc nhập. **Tối đa 250 ký tự**. |
| **Mã thuộc tính** | Text Field | Tự động sinh nếu để trống. Nếu nhập thủ công: **Tối đa 250 ký tự**. |
| **Mã Màu (HEX)** | Text Field | Dành cho Màu sắc. **Tối đa 250 ký tự**. |
| **Kích thước (Size)** | Number Field | **Bắt buộc là số nguyên**. Hệ thống tự động chặn gõ chữ cái hoặc dấu thập phân (dấu chấm `.`). **Tối đa 250 ký tự**. |
| **Mô tả** | Text Area | Không giới hạn ký tự. Tùy chọn. |

### 1.3. Quản lý Biến thể sản phẩm (Variant Management)
Mỗi sản phẩm thực tế là tập hợp của nhiều biến thể (Màu sắc + Kích thước).

* **Yêu cầu số lượng biến thể**: Khi **Thêm mới sản phẩm**, hệ thống yêu cầu bắt buộc phải tạo **ít nhất 1 biến thể** trước khi lưu.
* **Chống trùng lặp**: Không cho phép thêm 2 biến thể có **cùng Màu sắc và Kích thước** trong cùng một sản phẩm. Báo lỗi: *"Tổ hợp màu sắc và kích thước này đã tồn tại"*.
* **Áp dụng nhanh & Định dạng số**: Các ô nhập Số lượng, Giá bán, Giá nhập trong tính năng "Áp dụng nhanh" và trên từng dòng biến thể đều có định dạng ngăn cách hàng nghìn (VD: `1.000.000`).

| Tên trường của Biến thể | Loại Input | Quy tắc Validate (Rules) |
| --- | --- | --- |
| **Màu sắc** | Select | Bắt buộc chọn. |
| **Kích thước** | Select | Bắt buộc chọn. |
| **Số lượng (Tồn kho)** | Number Field | Bắt buộc nhập. Giá trị phải **$\ge 0$**. Có dấu phân cách hàng nghìn. |
| **Giá nhập** | Number Field | Bắt buộc nhập. Giá trị phải **$\ge 0$**. Có dấu phân cách hàng nghìn. |
| **Giá bán** | Number Field | Bắt buộc nhập. Giá trị phải **$\ge 0$**. Có dấu phân cách hàng nghìn. |

---

## 2. Màn hình Bán hàng (POS) (`BanHang.vue` & Components)

Màn hình bán hàng cho thu ngân được thiết kế để thao tác nhanh, do đó các validation thường chặn ngay ở mức Backend hoặc thao tác để tránh lỗi gián đoạn.

### 2.1. Thao tác trên Giỏ hàng (Cart)
| Thao tác | Quy tắc Validate & Xử lý |
| --- | --- |
| **Thêm sản phẩm vào giỏ** | - Số lượng thêm phải **> 0**.<br>- Hệ thống tự động kiểm tra tồn kho trực tiếp từ Database. Nếu tồn kho không đủ để đáp ứng, chặn thêm và báo lỗi: *"Sản phẩm đã hết hàng"* (`PRODUCT_OUT_OF_STOCK`). |
| **Sửa số lượng trong giỏ** | - Nếu người dùng nhập số lượng **$\le 0$**, hệ thống hiểu là muốn **Xóa sản phẩm** đó khỏi giỏ hàng.<br>- Nếu nhập số lượng **Lớn hơn tồn kho còn lại**, hệ thống tự động chặn lại và báo lỗi: *"Sản phẩm không đủ số lượng"* (`PRODUCT_INSUFFICIENT_QTY`). |
| **Xóa hóa đơn chờ** | - Sẽ tự động trả lại đúng số lượng của tất cả sản phẩm trong hóa đơn đó về lại kho. |

### 2.2. Panel Thanh toán (Checkout)
Trước khi cho phép thu ngân hoàn tất đơn hàng, hệ thống kiểm tra các điều kiện sau:

| Điều kiện kiểm tra | Quy tắc Validate & Báo lỗi |
| --- | --- |
| **Giỏ hàng rỗng** | Hóa đơn phải có **ít nhất 1 sản phẩm**. Nếu giỏ hàng trống, hệ thống chặn thanh toán và báo lỗi: *"Vui lòng thêm sản phẩm trước khi thanh toán"*. |
| **Thanh toán Tiền mặt (CASH)** | Số tiền **Khách đưa** (`receivedAmount`) phải **Lớn hơn hoặc Bằng** Tổng tiền thanh toán cuối cùng (`tongTienSauGiam`). Nếu nhập thiếu, hệ thống chặn và báo: *"Số tiền khách đưa không đủ"*. |
| **Thanh toán VNPay (VNPAY)** | Giao dịch qua mã QR sẽ được hệ thống kiểm tra đối chiếu Callback từ VNPay. Các tham số `vnp_ResponseCode` và `vnp_TransactionStatus` phải mang giá trị `"00"` (Thành công). Nếu chữ ký bảo mật sai hoặc trạng thái lỗi, hóa đơn sẽ không được chuyển sang trạng thái Hoàn thành. |
| **Thêm Khách hàng** | (Tùy chọn) Không bắt buộc. Nhưng nếu tìm kiếm khách hàng, hệ thống sẽ query theo số điện thoại hoặc tên. |
| **Áp dụng Mã giảm giá** | Chỉ được áp dụng các Voucher đang còn hạn, còn số lượng và thỏa mãn điều kiện **Giá trị đơn hàng tối thiểu** của voucher đó. |
