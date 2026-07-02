# AeroStride Preserve Functions Rule

Tài liệu này quy định tiêu chuẩn về việc bảo tồn các logic, hàm và phương thức đã có sẵn trong dự án AeroStride.

## 1. Không Thay Thế Hoặc Xóa Hàm Hiện Có (Do Not Replace Existing Functions)
**Vấn đề:** Việc tự ý thay thế, ghi đè hoặc xóa các hàm/phương thức đã có sẵn trong chương trình có thể gây ra lỗi hồi quy (regression bugs), làm hỏng các tính năng khác đang sử dụng chung hàm đó, và làm gián đoạn luồng nghiệp vụ hiện tại.

### Quy tắc:
- **Tôn trọng logic hiện tại:** Không được thay thế, sửa đổi làm thay đổi bản chất, xóa hoặc ghi đè các hàm/phương thức đã có sẵn trong chương trình trừ khi có yêu cầu cụ thể và rõ ràng từ người dùng.
- **Tái sử dụng:** Ưu tiên sử dụng lại các hàm đã có thay vì viết mới một hàm có chức năng tương tự, trừ khi hàm cũ không thể đáp ứng hoặc được yêu cầu refactor.
- **Mở rộng thay vì sửa đổi (Open/Closed Principle):** Nếu hàm hiện tại không đáp ứng đủ yêu cầu mới, hãy ưu tiên mở rộng (viết thêm hàm mới, overload) hoặc thêm tùy chọn (tham số cấu hình) thay vì thay đổi logic cốt lõi của hàm cũ, để đảm bảo không phá vỡ các nơi đang gọi nó.
- **Bảo tồn Comment/Docstring:** Luôn giữ lại các comment và docstring của các hàm hiện có nếu buộc phải thêm code vào bên trong hàm đó.
