---
name: smart-merge-preservation
description: Skill đảm bảo việc merge code không bị mất dữ liệu, ưu tiên giữ lại các cải tiến về UI/Logic và thực hiện "hợp nhất" thay vì "ghi đè".
---

# Smart Merge & Code Preservation

Skill này được thiết kế để ngăn chặn việc mất code khi thực hiện các thao tác merge, rebase hoặc cập nhật file từ các nhánh khác nhau. Nó đặc biệt quan trọng khi bạn (AI) thực hiện thay đổi trên một file mà người dùng cũng đã có những cải tiến quan trọng.

## Nguyên tắc cốt lõi

1.  **KHÔNG BAO GIỜ Ghi đè mù quáng (No Blind Overwrite)**: Trước khi thay đổi bất kỳ file nào, phải xem xét kỹ nội dung hiện tại. Nếu nội dung hiện tại có những logic hoặc style "xịn" hơn (dài hơn, chi tiết hơn, có animation...), phải tìm cách tích hợp chúng thay vì xóa bỏ.
2.  **Ưu tiên Code Đầy đủ (Prioritize Completeness)**: Nếu có sự khác biệt giữa hai phiên bản, hãy ưu tiên phiên bản có implementation chi tiết hơn (nhiều tính năng hơn, xử lý lỗi tốt hơn).
3.  **Hợp nhất Logic (Logic Union)**: Nếu nhánh A thêm tính năng X và nhánh B thêm tính năng Y, kết quả merge PHẢI có cả X và Y.
4.  **Bảo tồn UI Premium**: Các cải tiến về giao diện (CSS, Animations, Vuetify components mới) phải được giữ lại dù logic backend có thay đổi.

## Quy trình thực hiện Merge an toàn

### Bước 1: Phân tích đối sánh (Diff Analysis)
- Đọc file hiện tại và file từ nhánh cần merge.
- Xác định các khối code mới được thêm vào ở cả hai bên.
- Chú ý các hàm trùng tên nhưng có implementation khác nhau.

### Bước 2: Lập kế hoạch hợp nhất
- Xác định xem các thay đổi có xung đột trực tiếp không.
- Nếu không xung đột: Áp dụng cả hai.
- Nếu xung đột:
    - Nếu một bên là code placeholder (ví dụ: `// code cũ...`) và một bên là code thật: **Giữ code thật**.
    - Nếu cả hai đều là code thật: Hợp nhất logic của cả hai vào một hàm duy nhất hoặc tạo các hàm bổ trợ.

### Bước 3: Áp dụng thay đổi (Atomic Edits)
- Sử dụng `multi_replace_file_content` để thay đổi từng khối một cách chính xác.
- Tuyệt đối không dùng `replace_file_content` để ghi đè toàn bộ file nếu không chắc chắn 100% về việc bảo tồn code.

## Các kịch bản cụ thể

### 1. Merge UI Components
- **Tình huống**: Nhánh `main` có logic mới, nhưng nhánh `feature` có UI đẹp hơn (nhiều hiệu ứng, màu sắc premium).
- **Hành động**: Giữ nguyên toàn bộ phần `<template>` và `<style>` của nhánh `feature`, chỉ cập nhật các hàm xử lý trong `<script>` từ `main` sang.

### 2. Hợp nhất danh sách hàm/biến
- **Tình huống**: Cả hai nhánh đều thêm các biến vào `ref` hoặc `reactive`.
- **Hành động**: Khai báo tất cả các biến từ cả hai nhánh. Sắp xếp lại theo nhóm chức năng để dễ quản lý.

### 3. Xử lý hàm Lifecycle (onMounted, Watchers)
- **Tình huống**: Cả hai nhánh đều cần thực hiện logic khi `onMounted`.
- **Hành động**: Tạo một block `onMounted` duy nhất chứa code của cả hai nhánh, đảm bảo thứ tự gọi hàm hợp lý.

## Cảnh báo quan trọng (Critical Warnings)

- **Cấm dùng Placeholder**: Không bao giờ được phép để lại các đoạn code như `// ... existing code` trong kết quả cuối cùng.
- **Kiểm tra Imports**: Sau khi merge, phải kiểm tra lại danh sách `import` để không bị thiếu các thư viện mới hoặc dư thừa các thư viện đã xóa.
- **Ưu tiên người dùng**: Nếu code của người dùng viết dài hơn và chi tiết hơn, ĐỪNG THAY THẾ nó bằng code rút gọn của AI trừ khi logic đó sai.

---
*Lưu ý: Luôn đọc kỹ nội dung file trước khi thực hiện hành động để không biến "Premium UI" thành "Basic UI".*
