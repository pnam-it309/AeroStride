---
name: safe-commit-push
description: Quy định nghiêm ngặt về việc commit và push code. CHỈ thực hiện khi có lệnh rõ ràng từ USER.
---

# Quy Tắc Commit & Push Code Của AeroStride

Skill này quy định nghiêm ngặt các trường hợp được phép và không được phép commit hoặc push code lên Git. AI bắt buộc phải tuân thủ để tránh làm xáo trộn lịch sử git và gây khó khăn cho việc quản lý mã nguồn.

## 1. NGUYÊN TẮC CỐT LÕI
- **KHÔNG TỰ Ý COMMIT/PUSH:** Tuyệt đối không bao giờ chạy lệnh `git commit` hay `git push` tự động hoặc tự ý sau khi sửa code, trừ khi nhận được yêu cầu trực tiếp và rõ ràng từ USER.
- **YÊU CẦU PHẢI RÕ RÀNG:** Các lệnh như "Sửa cho t cái này", "Bổ sung case chất liệu", v.v. chỉ là lệnh sửa code, **không phải** lệnh commit hay push code.
- **XÁC NHẬN RÕ RÀNG:** Chỉ thực hiện commit/push khi USER chat trực tiếp các câu lệnh có ý nghĩa rõ ràng như:
  - *"Hãy commit code"*
  - *"Đẩy code lên git cho t"*
  - *"Commit và push nhé"*

## 2. QUY TRÌNH PHÁT TRIỂN CHUẨN (WORKFLOW)
1. **Bước 1: Viết/Sửa Code:** Thực hiện thay đổi các file nguồn theo yêu cầu của USER.
2. **Bước 2: Xác thực (Compile & Test):** Chạy `compileJava` hoặc các lệnh test tương ứng để đảm bảo code hoạt động hoàn hảo và không bị lỗi cú pháp.
3. **Bước 3: Báo cáo kết quả:** Gửi phản hồi mô tả chi tiết những gì đã thay đổi và hỏi USER có muốn commit/push hay không.
4. **Bước 4: Chờ lệnh:** Đợi USER phê duyệt. Chỉ chạy lệnh git commit/push khi được đồng ý.

## 3. CHECKLIST CHO AI TRƯỚC KHI CHẠY LỆNH GIT
- [ ] USER đã yêu cầu cụ thể việc commit/push chưa? (Nếu chưa -> DỪNG LẠI và KHÔNG CHẠY).
- [ ] Code đã compile thành công chưa?
- [ ] Đã sử dụng đúng định dạng Commit quy chuẩn chưa?

---
*Quy tắc này là tối thượng để bảo vệ kho lưu trữ mã nguồn của dự án.*
