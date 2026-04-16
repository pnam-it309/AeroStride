# Tài liệu Dự án AeroStride

## Tổng quan nhanh

AeroStride hiện được tổ chức thành hai khối chính:

- `BE/`: Backend Spring Boot 3.4.6, Java 17, JPA, Security, Redis, Flyway.
- `FE/`: Frontend Vue 3 + Vite + Vuetify + Pinia + Axios.

Repo này đã có tài liệu chi tiết trong `docs/`, và bây giờ có thêm bộ tài liệu root để làm "bản đồ dự án" tương tự repo tham chiếu ở `E:\DaTN\AeroStride`.

## Tài liệu chính

### 1. `PROJECT_OVERVIEW.md`

Tài liệu tổng quan toàn repo:

- Cấu trúc thư mục cấp cao
- Tech stack thực tế đọc từ source
- Bản đồ module nghiệp vụ
- Luồng request FE -> BE -> DB
- Auth, migration, test, quick trace

Dùng khi:

- Mới vào dự án
- Cần tìm nơi bắt đầu để sửa một module
- Cần hiểu ảnh hưởng của một thay đổi

### 2. `DEVELOPMENT_WORKFLOW.md`

Hướng dẫn làm việc hằng ngày:

- Chuẩn bị task
- Cách lần từ UI tới API
- Lệnh chạy local cho Windows
- Checklist trước khi commit và trước khi merge

Dùng khi:

- Bắt đầu task mới
- Cần quy trình ngắn gọn để tránh bỏ sót bước

### 3. `SECURITY_CHECKLIST.md`

Checklist bảo mật tập trung cho:

- JWT/auth
- Validation và phân quyền
- Upload file, secrets, dữ liệu nhạy cảm
- Review nhanh với các thay đổi ở API

Dùng khi:

- Sửa auth, payment, upload, nhân viên, khách hàng

### 4. `TROUBLESHOOTING.md`

Gợi ý xử lý các lỗi local phổ biến:

- Backend không lên
- Lỗi DB/Flyway
- 401/CORS
- FE không gọi được API

Dùng khi:

- Setup local lần đầu
- Debug lỗi môi trường

### 5. `MODIFICATION_LOG.md`

Mẫu ghi nhận thay đổi lớn để theo dõi tác động và lịch sử cập nhật.

Dùng khi:

- Làm refactor lớn
- Sửa flow nghiệp vụ có nhiều module liên quan

### 6. `docs/backend_doc.md` và `docs/frontend_doc.md`

Hai file mô tả cấu trúc chi tiết của backend/frontend.

Dùng khi:

- Cần biết module đang theo pattern nào
- Cần tạo file mới đúng vị trí

## Thứ tự đọc gợi ý

### Muốn hiểu dự án nhanh

1. `PROJECT_OVERVIEW.md`
2. `docs/backend_doc.md`
3. `docs/frontend_doc.md`

### Muốn bắt đầu làm task mới

1. `PROJECT_OVERVIEW.md`
2. `DEVELOPMENT_WORKFLOW.md`
3. File module tương ứng trong `docs/`

### Muốn debug bug

1. `PROJECT_OVERVIEW.md`
2. `TROUBLESHOOTING.md`
3. `SECURITY_CHECKLIST.md` nếu bug liên quan auth/quyền

## Ghi chú

- Repo hiện có thay đổi local ở `BE/env/env.dev` và `BE/src/test/resources/application.properties`; các tài liệu này không can thiệp vào các file đó.
- Bộ tài liệu này bám theo code thật ở repo hiện tại, không sao chép nguyên trạng từ repo tham chiếu.
