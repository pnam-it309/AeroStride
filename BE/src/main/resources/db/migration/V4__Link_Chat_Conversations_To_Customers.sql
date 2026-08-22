-- ==============================================================================
-- V4: Gán các cuộc hội thoại của khách hàng đã đăng nhập về đúng tài khoản
-- Phiên chat của khách đã đăng nhập dùng ma_phien = 'user_<ten_tai_khoan>',
-- trước đây không lưu id_khach_hang nên phía nhân viên hiển thị "Khách vãng lai".
-- ==============================================================================

UPDATE cuoc_hoi_thoai c
JOIN khach_hang k ON c.ma_phien = CONCAT('user_', k.ten_tai_khoan)
SET c.id_khach_hang = k.id
WHERE c.id_khach_hang IS NULL;
