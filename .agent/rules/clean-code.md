# AeroStride Clean Code Standards & Rules

Tài liệu này quy định các tiêu chuẩn viết code để đảm bảo dự án AeroStride luôn duy trì chất lượng cao, dễ bảo trì và mở rộng. Mọi thành viên (và AI) phải tuân thủ nghiêm ngặt.

## 1. Không Hardcode (No Hardcoding)
**Vấn đề:** Hardcode khiến code khó thay đổi, dễ gây lỗi khi cần cập nhật ở nhiều nơi và làm giảm tính rõ ràng.

### Quy tắc:
- **API Paths:** Tuyệt đối không viết trực tiếp URL string trong Controller hoặc Service.
    - BE: Sử dụng `infrastructure/constants/RoutesConstant.java`.
    - FE: Sử dụng `constants/RouteConstants.js`.
- **Magic Numbers/Strings:** Không sử dụng các con số hoặc chuỗi ký tự có ý nghĩa đặc biệt (ví dụ: `status == 1`, `role == "ADMIN"`) trực tiếp trong logic.
    - BE: Sử dụng Enum hoặc `public static final` constants.
    - FE: Sử dụng file trong `constants/` hoặc Enums.
- **Messages/Labels:** Các thông báo lỗi, nhãn giao diện nên được quản lý tập trung (I18n hoặc Constants) thay vì viết cứng trong template/logic.

## 2. Không Lặp Code (DRY - Don't Repeat Yourself)
**Vấn đề:** Lặp code tạo ra "nợ kỹ thuật" (technical debt). Khi cần sửa một logic, bạn phải sửa ở tất cả các nơi lặp lại, dẫn đến nguy cơ bỏ sót.

### Quy tắc:
- **Logic Nghiệp vụ:** Nếu một logic tính toán hoặc xử lý dữ liệu xuất hiện từ 2 lần trở lên, hãy tách nó ra:
    - BE: Đưa vào `utils/` hoặc một Service chung.
    - FE: Đưa vào `utils/` hoặc `composables/`.
- **UI Components:** Không copy-paste code HTML/CSS của các thành phần giao diện (Table, Modal, Button).
    - FE: Nếu thấy một UI pattern lặp lại, hãy tạo/sử dụng component trong `components/base/` hoặc `components/shared/`.
- **Query SQL/JPA:** Tránh viết cùng một câu truy vấn ở nhiều Repository khác nhau. Sử dụng các phương thức dùng chung hoặc Query Fragments.

## 3. Không Làm Custom Code Thừa (Avoid Redundant Custom Code)
**Vấn đề:** Tự viết lại những thứ mà Framework (Spring Boot, Vue, Vuetify) hoặc thư viện đã hỗ trợ sẵn làm tốn thời gian và dễ gây lỗi.

### Quy tắc:
- **Tận dụng Framework:**
    - BE: Sử dụng Spring Data JPA (Query Methods, Specifications) thay vì viết Native SQL thủ công cho các truy vấn đơn giản. Sử dụng Lombok để giảm boilerplate code (Getter, Setter, Constructor).
    - FE: Sử dụng đầy đủ các tính năng của Vuetify 3 (v-data-table, v-form validation, grid system) thay vì tự viết CSS/JS custom cho layout và validation.
- **Kiểm tra Tiện ích Hiện có:** Trước khi viết một hàm helper (format date, format tiền tệ, xử lý chuỗi), hãy kiểm tra thư mục `utils/` hoặc `composables/` xem đã có sẵn chưa.
- **Xử lý Lỗi:** Sử dụng hệ thống `Global Exception Handler` (BE) và `Interceptors` (FE) đã có sẵn. Đừng viết `try-catch` rườm rà ở mọi nơi chỉ để log lỗi.

## 4. Kiểm tra và Thực thi (Enforcement)
- **AI Assistant:** Khi thực hiện bất kỳ task nào, AI phải tự check lại code so với các quy tắc này. Nếu phát hiện vi phạm, phải chủ động refactor ngay lập tức.
- **Code Review:** Mọi Pull Request phải được kiểm tra dựa trên danh sách này trước khi merge.

---
*Cập nhật lần cuối: 2026-05-12*
