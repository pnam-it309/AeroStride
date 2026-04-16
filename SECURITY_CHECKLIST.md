# Checklist Bảo mật AeroStride

## 1. Auth và phân quyền

- Endpoint mới đã được đặt đúng nhóm route chưa
- API nhạy cảm đã kiểm quyền ở backend chưa
- FE guard có đang cho đi nhầm route riêng tư không
- 401/403 có được xử lý nhất quán giữa FE và BE không
- JWT secret có đang lấy từ env thay vì hard-code không

## 2. Input và validation

- Request body có DTO/validation tương ứng không
- Có chặn dữ liệu rỗng, sai kiểu, vượt giới hạn không
- Các filter/search có tránh query quá rộng hoặc thiếu kiểm soát không

## 3. Dữ liệu nhạy cảm

- Không commit secrets vào repo
- Không log password, token, hash secret, thông tin thanh toán
- Không trả dữ liệu nội bộ thừa trong response

## 4. Upload và file

- File upload có kiểm loại file và kích thước không
- Đường dẫn file có được chuẩn hóa và tránh path traversal không
- Nếu dùng Cloudinary/local storage, đã kiểm quyền truy cập file chưa

## 5. Database và migration

- Không ghép chuỗi query thiếu kiểm soát
- Migration mới có an toàn với dữ liệu cũ không
- Không dùng dữ liệu mẫu/test cho môi trường thật

## 6. FE lưu token và session

- Token đang nằm trong `sessionStorage`; mọi thay đổi auth cần kiểm lại rủi ro XSS
- Logout có xóa dữ liệu phiên đúng cách không
- Redirect sau khi hết phiên có rõ ràng không

## 7. Thanh toán và khuyến mại

- Rule giảm giá/voucher có được kiểm ở backend, không chỉ ở FE
- Không tin hoàn toàn dữ liệu số tiền do FE gửi lên
- Mọi thay đổi liên quan payment cần kiểm thêm luồng thất bại và rollback

## 8. Trước khi merge

- Đã rà lại file config/env bị chạm tới
- Đã test ít nhất một case hợp lệ và một case bị chặn
- Đã xem nhanh log/error handling cho route vừa đổi
