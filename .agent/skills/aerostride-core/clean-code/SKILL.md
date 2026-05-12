---
name: clean-code-standards
description: Enforce Clean Code standards (No hardcoding, DRY, No redundant code) for AeroStride.
---

# AeroStride Clean Code Skill

Sử dụng skill này để đảm bảo code luôn sạch, không hardcode, không lặp và không thừa.

## 1. MỤC TIÊU
- Loại bỏ hardcode (Strings, Numbers, API Paths).
- Áp dụng nguyên lý DRY (Don't Repeat Yourself).
- Tận dụng tối đa các tính năng có sẵn của Framework (Spring Boot, Vue, Vuetify).

## 2. QUY TẮC CHI TIẾT
Tham khảo bộ quy tắc đầy đủ tại: [clean-code.md](file:///d:/AeroStride/.agent/rules/clean-code.md)

### Checklist khi viết code:
- [ ] **Hardcode:** Đã chuyển các chuỗi/số có nghĩa thành hằng số (Constants) hoặc Enum chưa? Các API path đã dùng `RoutesConstant.java` (BE) hoặc `RouteConstants.js` (FE) chưa?
- [ ] **Trùng lặp:** Logic này có đang bị lặp lại ở đâu không? Có thể tách ra `utils` hoặc `composables` không?
- [ ] **Tận dụng Framework:** Có đang viết custom CSS/JS cho những thứ Vuetify đã hỗ trợ (ví dụ: validation, layout) không? Có đang viết SQL thủ công cho các CRUD đơn giản không?

## 3. CÁCH THỰC HIỆN
1. **Trước khi code:** Quét qua các file `utils/`, `constants/`, và `composables/` để xem logic định viết đã tồn tại chưa.
2. **Trong khi code:** Nếu thấy mình đang gõ một chuỗi URL hoặc một con số "magic", hãy dừng lại và tạo hằng số.
3. **Sau khi code:** Tự review lại dựa trên checklist trên.

---
*Mọi đoạn code do AI tạo ra PHẢI tuân thủ các tiêu chuẩn này.*
