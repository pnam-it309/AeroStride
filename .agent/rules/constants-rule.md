# AeroStride Constant Enforcement Rules

Tài liệu này quy định quy tắc bắt buộc về việc kiểm tra, khai báo và tái sử dụng Constants trong toàn bộ hệ thống AeroStride (Frontend & Backend).

---

## 1. Nguyên Tắc Cốt Lõi (Core Principles)

1. **Tuyệt đối không Hardcode (Zero Hardcoded Magic Strings/Numbers)**:
   - Nghiêm cấm viết trực tiếp các chuỗi role (`"ROLE_QUAN_LY"`, `"ROLE_NHAN_VIEN"`, `"ADMIN"`...), mã trạng thái (`1`, `2`, `"ACTIVE"`, `"CHO_XAC_NHAN"`...), mã router (`"/admin/nhan-vien"`...), mã API (`"/api/v1/admin/..."`) trực tiếp trong template, controller, service, store hay guard.
2. **Kiểm tra Constants trước khi code (Check Before Write)**:
   - **TRƯỚC KHI** sử dụng bất kỳ chuỗi định danh hay giá trị nào, **BẮT BUỘC** phải kiểm tra thư mục constants tương ứng xem hằng số hoặc helper function đã tồn tại hay chưa.
   - Nếu đã có: **BẮT BUỘC** import và sử dụng constant/helper đó.
   - Nếu chưa có: Khai báo mới vào file constants tập trung phù hợp trước khi sử dụng.

---

## 2. Bản Đồ Constants (Constants Mapping)

### A. Phía Frontend (`FE/src/constants/`)
| Phạm vi / Đối tượng | File Constants | Ví dụ hằng số / Hàm helper |
| :--- | :--- | :--- |
| **Phân quyền & Vai trò** | `appConstants.js` | `APP_ROLES`, `ROLE_CODES`, `isRoleAdmin()`, `isRoleStaff()`, `isRoleCustomer()`, `isManagementRole()` |
| **Đường dẫn Route** | `routePaths.js` | `PATH.DASHBOARD`, `PATH.NHAN_VIEN`, `PATH.BAN_HANG`, `PATH.DANH_GIA`... |
| **Đường dẫn API** | `apiPaths.js` | `API_ADMIN.NHAN_VIEN`, `API_AUTH.LOGIN`, `API_LICH_LAM_VIEC`... |
| **Trạng thái hệ thống** | `statusConstants.js` | `SYSTEM_STATUS.ACTIVE`, `SYSTEM_STATUS.INACTIVE`... |
| **Icons quản trị** | `adminIcons.js` | `ADMIN_ICONS.ACTION.EDIT`, `ADMIN_ICONS.ACTION.DELETE`... |
| **Module Nhân viên** | `nhanVienConstants.js` | `TRANG_THAI_NHAN_VIEN`, `NHAN_VIEN_MESSAGES`... |
| **Module Đánh giá** | `danhGiaConstants.js` | `DANH_GIA_STATUS`, `DANH_GIA_RATING_OPTIONS`... |

### B. Phía Backend (`BE/src/main/java/com/example/be/infrastructure/constants/`)
| Phạm vi / Đối tượng | File Constants | Ví dụ hằng số / Enum |
| :--- | :--- | :--- |
| **Vai trò & Security** | `VaiTro.java` | `VaiTro.STAFF`, `VaiTro.ADMIN`, `VaiTro.CUSTOMER`, `VaiTro.PRE_AUTH_ADMIN_STAFF`, `VaiTro.isManagementRole()` |
| **Routing / Endpoints** | `RoutesConstant.java` | `RoutesConstant.ADMIN_NHAN_VIEN`, `RoutesConstant.AUTH`, `RoutesConstant.HIEN_THI`... |
| **Trạng thái Entity** | `TrangThai.java` | `TrangThai.DANG_HOAT_DONG`, `TrangThai.NGUNG_HOAT_DONG`... |
| **Thông điệp hệ thống** | `MessageConstants.java` | `MessageConstants.NHAN_VIEN_ADD_SUCCESS`, `MessageConstants.UNAUTHORIZED`... |

---

## 3. Checklist Thực Thi (Enforcement Checklist)

Mỗi khi AI thực hiện chỉnh sửa hoặc tạo mới code:
- [ ] Đã kiểm tra file `appConstants.js` / `VaiTro.java` trước khi so sánh quyền/vai trò chưa?
- [ ] Có còn chuỗi string/number nào bị viết cứng (`"ADMIN"`, `"ROLE_NHAN_VIEN"`, `0`, `1`) không?
- [ ] Các đường dẫn router/API đã sử dụng `PATH` và `API_ADMIN` / `RoutesConstant` chưa?
- [ ] Các trạng thái đã dùng `SYSTEM_STATUS` / `TrangThai` chưa?
