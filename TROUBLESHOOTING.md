# Khắc phục Lỗi Thường gặp AeroStride

## 1. Backend không khởi động được

Kiểm tra:

- Đã có file env phù hợp trong `BE/env/`
- `SPRING_DATASOURCE_URL`, username, password đã đúng
- `JWT_SECRET` đã được set
- MySQL và Redis đã chạy

Lệnh gợi ý:

```powershell
cd BE
.\gradlew.bat bootRun
```

## 2. FE gọi API bị 401 liên tục

Kiểm tra:

- Token có nằm trong `sessionStorage` không
- FE đang redirect về `/auth/login` do interceptor hay do guard
- Backend có nhận đúng header `Authorization` không
- Role của tài khoản có phù hợp endpoint không

File nên xem:

- `FE/src/services/apiService.js`
- `FE/src/router/guards.js`
- `BE/src/main/java/com/example/be/infrastructure/security/*`

## 3. FE không gọi được API hoặc dính CORS

Kiểm tra:

- `VITE_API_URL` có đúng không
- `allowed.origin` trong backend có khớp domain FE local không
- FE đang dùng `'/api/v1'` mặc định hay env riêng

## 4. Lỗi kết nối database

Kiểm tra:

- DB đã chạy và đúng port chưa
- Tài khoản DB trong env có đúng quyền không
- Tên database có tồn tại chưa

File nên xem:

- `BE/src/main/resources/application.properties`
- `BE/env/.env.example`

## 5. Lỗi Flyway hoặc schema không khớp

Kiểm tra:

- Có migration mới nhưng DB local chưa áp dụng không
- Có chỉnh trực tiếp schema làm lệch với migration không
- Có dùng dữ liệu cũ không tương thích với source hiện tại không

## 6. FE route mở được nhưng màn hình trắng

Kiểm tra:

- Route khai báo trong `FE/src/router/MainRoutes.js`
- Component import path có đúng không
- Service gọi API có ném lỗi nhưng bị nuốt ở console không

## 7. Login xong nhưng không vào được `/main`

Kiểm tra:

- `dichVuXacThuc.daDangNhap()` có trả về đúng không
- Guard `requireAuth` và `requireGuest` có hoạt động đúng không
- Token có bị xóa ngay sau login vì interceptor nhận 401 không

## 8. Cần trace bug nhanh

Làm theo thứ tự:

1. Xác định route FE
2. Xác định FE service
3. Xác định controller BE
4. Đọc service impl
5. Kiểm repository/entity và dữ liệu DB
