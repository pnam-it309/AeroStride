-- =========================================================================
-- V4: Cập nhật quyền Quản lý (QUAN_LY) cho 5 tài khoản nhân viên / quản trị mẫu
-- =========================================================================

-- 1. Đảm bảo bản ghi quyền QUAN_LY (pq1) chuẩn xác
INSERT INTO phan_quyen (id, ma_phan_quyen, ten_phan_quyen, quyen_han, mo_ta, trang_thai, ngay_tao)
VALUES ('pq1', 'QUAN_LY', 'Quản lý', 'MANAGEMENT_ACCESS', 'Quyền quản lý cửa hàng và hệ thống', 0, 1711814400000)
ON DUPLICATE KEY UPDATE 
    ma_phan_quyen = 'QUAN_LY',
    ten_phan_quyen = 'Quản lý',
    quyen_han = 'MANAGEMENT_ACCESS';

-- 2. Cập nhật 5 nhân viên / tài khoản admin sang quyền Quản lý (pq1)
UPDATE nhan_vien 
SET id_phan_quyen = 'pq1' 
WHERE id IN ('nv1', 'nv2', 'nv3', 'nv4', 'nv5')
   OR ma_nhan_vien IN ('NV001', 'NV002', 'NV003', 'NV004', 'NV005')
   OR ten_tai_khoan IN ('admin', 'admin1', 'admin2', 'admin3', 'admin4');
