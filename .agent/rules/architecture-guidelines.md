# AeroStride Technical & Architectural Guidelines

Tài liệu này quy định các tiêu chuẩn kỹ thuật, kiến trúc hệ thống và quy trình làm việc chi tiết cho dự án AeroStride. Mọi thành viên đội ngũ phát triển và AI Agent (Antigravity) phải tuân thủ tuyệt đối khi phát triển tính năng, sửa lỗi hoặc cấu trúc lại mã nguồn.

---

## 🏛️ 1. Kiến Trúc Hệ Thống (System Architecture)

Dự án AeroStride được xây dựng theo mô hình **Client-Server** phân tách hoàn chỉnh:
*   **Backend (BE):** Java Spring Boot 3.x, sử dụng build tool Gradle, kết nối cơ sở dữ liệu qua JPA/Hibernate.
*   **Frontend (FE):** Vue 3 (Composition API), Vite, Pinia (State Management), Vuetify 3 (UI Component Library).
*   **Automation Tests:** Playwright cho cả API Testing và UI E2E Testing.

---

## ☕ 2. Quy Tắc Phát Triển Backend (Spring Boot Guidelines)

### 2.1 Cấu Trúc Phân Lớp (Layered Architecture)
Tất cả mã nguồn Java phải tuân thủ nghiêm ngặt mô hình 4 lớp tiêu chuẩn:
1.  **Controller Layer (`@RestController`):**
    *   Chỉ chịu trách nhiệm định tuyến, nhận HTTP Request, validate dữ liệu đầu vào (`@Valid`) và trả về HTTP Response.
    *   **Không** chứa logic nghiệp vụ (business logic) hoặc gọi trực tiếp sang các Repository.
    *   Sử dụng DTO (Data Transfer Object) thay vì truyền trực tiếp Entity ra ngoài API.
2.  **Service Layer (`@Service`):**
    *   Nơi xử lý toàn bộ logic nghiệp vụ, tính toán, phân quyền và điều phối luồng dữ liệu.
    *   Áp dụng `@Transactional(readOnly = true)` cho các hàm đọc và `@Transactional` cho các hàm ghi (create/update/delete).
3.  **Repository Layer (`@Repository`):**
    *   Tương tác trực tiếp với Database thông qua Spring Data JPA.
    *   Sử dụng JPA Query Methods hoặc `Specification` cho các truy vấn động phức tạp. Hạn chế sử dụng Native SQL trừ trường hợp bất khả kháng để tối ưu hiệu năng.
4.  **Entity Layer (`@Entity`):**
    *   Đại diện cho cấu trúc bảng trong Database.
    *   Sử dụng Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`) để giảm boilerplate code. **Tránh** dùng `@Data` trên các Entity có mối quan hệ `@OneToMany` hoặc `@ManyToMany` để tránh lỗi vòng lặp vô hạn trong `toString()` hoặc `hashCode()`.

### 2.2 Quy Tắc Đặt Tên (Naming Conventions)
*   **Class:** PascalCase (Ví dụ: `UserService`, `FlightController`, `BookingRepository`).
*   **Method & Variable:** camelCase (Ví dụ: `findFlightById`, `totalPrice`, `isAvailable`).
*   **Constants:** UPPER_SNAKE_CASE (Ví dụ: `MAX_RETRY_ATTEMPTS`, `DEFAULT_PAGE_SIZE`).
*   **Database Table:** snake_case số nhiều (Ví dụ: `users`, `flights`, `bookings`).
*   **Database Column:** snake_case số ít (Ví dụ: `first_name`, `flight_number`, `created_at`).

### 2.3 Xử Lý Ngoại Lệ (Exception Handling)
*   Sử dụng hệ thống **Global Exception Handler** thông qua `@RestControllerAdvice`.
*   Không lạm dụng khối `try-catch` chỉ để in log rồi ném lại lỗi. Hãy để ngoại lệ nổi lên lớp Controller để Global Handler bắt và định dạng response trả về cho Client một cách đồng nhất.
*   Định nghĩa các Custom Business Exception rõ ràng đại diện cho các lỗi nghiệp vụ (ví dụ: `ResourceNotFoundException`, `InvalidBookingException`).

---

## 🎨 3. Quy Tắc Phát Triển Frontend (Vue 3 & Vuetify 3 Guidelines)

### 3.1 Cấu Trúc Thành Phần (Component Structure)
*   Sử dụng **Composition API** với cú pháp `<script setup>` cho tất cả các component mới.
*   **Phân chia component:**
    *   `components/base/` hoặc `components/shared/`: Component dùng chung, thuần UI, nhận data qua `props` và truyền event ra ngoài qua `emits`. Không chứa logic nghiệp vụ đặc thù.
    *   `views/` hoặc `components/features/`: Component nghiệp vụ chứa logic cụ thể, tương tác với Pinia Store hoặc gọi API trực tiếp qua Axios.

### 3.2 State Management (Pinia Store)
*   Sử dụng Pinia cho các trạng thái dùng chung toàn hệ thống (User Profile, Giỏ hàng, Cấu hình giao diện).
*   Tránh lạm dụng truyền props quá sâu (Prop Drilling). Nếu dữ liệu cần dùng ở nhiều component con cách biệt xa, hãy lưu trữ trong Pinia Store.

### 3.3 Giao Diện & Tiện Ích Vuetify 3
*   Tận dụng tối đa hệ thống Grid System của Vuetify (`v-container`, `v-row`, `v-col`) để đảm bảo Responsive hoàn hảo trên Mobile, Tablet và Desktop.
*   Sử dụng các class tiện ích có sẵn (Spacing, Flexbox, Typography, Colors) của Vuetify thay vì viết custom CSS.
    *   *Ví dụ tốt:* `<div class="d-flex align-center ma-4 pa-2">`
    *   *Ví dụ xấu:* `<div style="display: flex; align-items: center; margin: 16px; padding: 8px;">`

### 3.4 Validate Form
*   Sử dụng `@vuelidate/core` kết hợp các hàm kiểm tra từ `@vuelidate/validators` hoặc schema `yup` để xác thực form đầu vào trước khi submit lên Server.

---

## 🚀 4. Quy Trình Phối Hợp & Quy Tắc Git (Git Flow & Commits)

### 4.1 Quy Tắc Đặt Tên Nhánh (Branch Naming)
Mọi thay đổi phải được phát triển trên nhánh tính năng, không commit trực tiếp vào nhánh `main` hoặc `develop`.
*   **Feature:** `feat/issue-[ID]-[tên-ngắn]` (Ví dụ: `feat/issue-102-oauth2-login`)
*   **Bugfix:** `fix/issue-[ID]-[tên-ngắn]` (Ví dụ: `fix/issue-304-button-alignment`)
*   **Refactor:** `refactor/issue-[ID]-[tên-ngắn]` (Ví dụ: `refactor/issue-201-clean-controllers`)

### 4.2 Định Dạng Commit (Conventional Commits)
Thông điệp commit phải tuân thủ định dạng sau:
`type(scope): description`

Các `type` được phép sử dụng:
*   `feat`: Thêm tính năng mới.
*   `fix`: Sửa lỗi.
*   `docs`: Cập nhật tài liệu kỹ thuật hoặc markdown.
*   `style`: Sửa định dạng code (whitespace, format, missing semi-colon...) không ảnh hưởng logic.
*   `refactor`: Cơ cấu lại mã nguồn hiện tại nhưng không thay đổi hành vi hoặc thêm tính năng.
*   `test`: Viết thêm unit test hoặc sửa bộ test.
*   `chore`: Cập nhật cấu hình build, dependencies hoặc build tools (Gradle, npm).

> [!IMPORTANT]
> **Co-Authored Attribution:**
> Khi các thay đổi được thực hiện bởi AI Agent (Antigravity), bắt buộc phải thêm thông tin Co-Authored vào phần footer của commit message để ghi nhận đóng góp:
> ```text
> Co-Authored-By: Antigravity <noreply@google.com>
> ```

---

## 🧪 5. Quy Tắc Kiểm Thử (Testing Standards)

Đảm bảo mã nguồn được bảo vệ bằng các tầng kiểm thử tương ứng:
1.  **Unit Tests (Backend):** Sử dụng JUnit 5 và Mockito. Viết test cho toàn bộ Service Layer với độ bao phủ (Coverage) mong muốn tối thiểu **80%**.
2.  **Component Tests (Frontend):** Sử dụng Vitest và `@vue/test-utils` để kiểm tra độ tin cậy của các component UI.
3.  **End-to-End & API Tests:** Viết và cập nhật kịch bản kiểm thử tự động bằng Playwright nằm trong thư mục `automation-tests` khi bổ sung API mới hoặc cập nhật luồng giao diện chính.

---
*Cập nhật lần cuối: 2026-05-20*
