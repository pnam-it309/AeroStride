-- Dữ liệu mẫu hoàn chỉnh (Full Sample Data - 5 bản ghi mỗi bảng)

-- 1. Bảng Phân Quyền (phan_quyen)
INSERT INTO phan_quyen (id, ma_phan_quyen, ten_phan_quyen, quyen_han, mo_ta, trang_thai, ngay_tao) VALUES
('pq1', 'ADMIN', 'Quản trị viên', 'FULL_ACCESS', 'Quyền cao nhất hệ thống', 0, 1711814400000),
('pq2', 'STAFF', 'Nhân viên', 'STAFF_ACCESS', 'Quyền nhân viên bán hàng', 0, 1711814400000),
('pq3', 'MANAGER', 'Quản lý', 'MANAGER_ACCESS', 'Quyền quản lý cửa hàng', 0, 1711814400000),
('pq4', 'WAREHOUSE', 'Thủ kho', 'WAREHOUSE_ACCESS', 'Quyền quản lý kho hàng', 0, 1711814400000),
('pq5', 'SHIPPER', 'Giao hàng', 'SHIPPER_ACCESS', 'Quyền nhân viên giao nhận', 0, 1711814400000);

-- 2. Bảng Nhân Viên (nhan_vien)
INSERT INTO nhan_vien (id, id_phan_quyen, ma_nhan_vien, ten_nhan_vien, email, sdt, ngay_sinh, gioi_tinh, ten_tai_khoan, mat_khau, hinh_anh, xoa_mem, trang_thai, ngay_tao) VALUES
('nv1', 'pq1', 'NV001', 'Admin Hệ Thống', 'nickhunter3009@gmail.com', '0123456789', '1990-01-01', 1, 'admin', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'admin.jpg', 0, 0, 1711814400000),
('nv2', 'pq1', 'NV002', 'Admin Hệ Thống', 'phitrang082006@gmail.com', '0123456789', '1990-01-01', 0, 'admin1', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'admin.jpg', 0, 0, 1711814400000),
('nv3', 'pq1', 'NV003', 'Admin Hệ Thống', 't818721@gmail.com', '0123456789', '1990-01-01', 0, 'admin2', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'admin.jpg', 0, 0, 1711814400000),
('nv4', 'pq1', 'NV004', 'Admin Hệ Thống', 'nguyenhuyducbg19062002@gmail.com', '0123456789', '1990-01-01', 1, 'admin3', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'admin.jpg', 0, 0, 1711814400000),
('nv5', 'pq1', 'NV005', 'Admin Hệ Thống', 'yent6969@gmail.com', '0123456789', '1990-01-01', 0, 'admin4', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'admin.jpg', 0, 0, 1711814400000),
('nv6', 'pq2', 'NV006', 'Nguyễn Văn Staff', 'staff1@aerostride.com', '0987654321', '1995-05-20', 1, 'staff1', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'staff1.jpg', 0, 0, 1711814400000),
('nv7', 'pq3', 'NV007', 'Trần Thị Manager', 'manager@aerostride.com', '0912345678', '1988-10-15', 0, 'manager', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'manager.jpg', 0, 0, 1711814400000),
('nv8', 'pq2', 'NV008', 'Lê Văn Bán Hàng', 'staff2@aerostride.com', '0922334455', '1998-02-14', 1, 'staff2', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'staff2.jpg', 0, 0, 1711814400000),
('nv9', 'pq4', 'NV009', 'Phạm Thủ Kho', 'warehouse@aerostride.com', '0933445566', '1992-07-30', 1, 'warehouse', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 'warehouse.jpg', 0, 0, 1711814400000);

-- 3. Bảng Địa Chỉ (dia_chi)
INSERT INTO dia_chi (id, ma_dia_chi, tinh, thanh_pho, phuong_xa, dia_chi_chi_tiet, ten_nguoi_nhan, sdt_nguoi_nhan, trang_thai, ngay_tao) VALUES
('dc1', 'DC001', 'Hà Nội', 'Quận Cầu Giấy', 'Phường Dịch Vọng', 'Số 123 Cầu Giấy', 'Nguyễn Khách 1', '0901112222', 0, 1711814400000),
('dc2', 'DC002', 'TP.HCM', 'Quận 1', 'Phường Bến Nghé', '45 Lê Lợi', 'Trần Anh Khách', '0902223333', 0, 1711814400000),
('dc3', 'DC003', 'Đà Nẵng', 'Quận Hải Châu', 'Phường Hòa Cường Bắc', '78 Phan Châu Trình', 'Lê Khách 3', '0903334444', 0, 1711814400000),
('dc4', 'DC004', 'Hải Phòng', 'Quận Hồng Bàng', 'Phường Hoàng Văn Thụ', '90 Đinh Tiên Hoàng', 'Phạm Khách 4', '0904445555', 0, 1711814400000),
('dc5', 'DC005', 'Cần Thơ', 'Quận Ninh Kiều', 'Phường An Lạc', '12 Nguyễn Trãi', 'Hoàng Khách 5', '0905556666', 0, 1711814400000);

-- 4. Bảng Khách Hàng (khach_hang)
INSERT INTO khach_hang (id, id_dia_chi, ma_nguoi_dung, ten_nguoi_dung, email, ten_tai_khoan, mat_khau, gioi_tinh, sdt, ngay_sinh, hinh_anh, ghi_chu, trang_thai, ngay_tao) VALUES
('kh1', 'dc1', 'KH001', 'Nguyễn Khách 1', 'khach1@gmail.com', 'khach1', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 1, '0901112222', '1993-12-01', 'kh1.jpg', 'Khách hay mua hàng', 0, 1711814400000),
('kh2', 'dc2', 'KH002', 'Trần Anh Khách', 'khach2@gmail.com', 'khach2', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 1, '0902223333', '1991-03-15', 'kh2.jpg', NULL, 0, 1711814400000),
('kh3', 'dc3', 'KH003', 'Lê Khách 3', 'khach3@gmail.com', 'khach3', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 0, '0903334444', '1995-07-20', 'kh3.jpg', NULL, 0, 1711814400000),
('kh4', 'dc4', 'KH004', 'Phạm Khách 4', 'khach4@gmail.com', 'khach4', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 0, '0904445555', '1998-05-10', 'kh4.jpg', NULL, 0, 1711814400000),
('kh5', 'dc5', 'KH005', 'Hoàng Khách 5', 'khach5@gmail.com', 'khach5', '{bcrypt}$2a$10$oocVPP6YqNyiSKxcgIZK4OgYXwGLfOVsShJcYSrUl55luGoRPr5rq', 1, '0905556666', '1990-11-25', 'kh5.jpg', NULL, 0, 1711814400000);

-- 5. Bảng Xuất xứ (xuat_xu)
INSERT INTO xuat_xu (id, ma_xuat_xu, ten_xuat_xu, trang_thai, xoa_mem, ngay_tao) VALUES
('xx1', 'XX001', 'Việt Nam', 0, 0, 1711814400000),
('xx2', 'XX002', 'Trung Quốc', 0, 0, 1711814400000),
('xx3', 'XX003', 'Mỹ', 0, 0, 1711814400000);

-- 6. Bảng Mục đích chạy (muc_dich_chay)
INSERT INTO muc_dich_chay (id, ma_muc_dich_chay, ten_muc_dich_chay, trang_thai, xoa_mem, ngay_tao) VALUES
('md1', 'MD001', 'Chạy Đường Dài', 0, 0, 1711814400000),
('md2', 'MD002', 'Chạy Tốc Độ', 0, 0, 1711814400000),
('md3', 'MD003', 'Luyện Tập Hàng Ngày', 0, 0, 1711814400000);

-- 7. Bảng Thương Hiệu (thuong_hieu)
INSERT INTO thuong_hieu (id, ma_thuong_hieu, ten_thuong_hieu, trang_thai, xoa_mem, ngay_tao) VALUES
('th1', 'TH001', 'Nike', 0, 0, 1711814400000),
('th2', 'TH002', 'Adidas', 0, 0, 1711814400000),
('th3', 'TH003', 'Puma', 0, 0, 1711814400000),
('th4', 'TH004', 'New Balance', 0, 0, 1711814400000),
('th5', 'TH005', 'Converse', 0, 0, 1711814400000);

-- 6. Bảng Danh Mục (danh_muc)
INSERT INTO danh_muc (id, ma_danh_muc, ten_danh_muc, trang_thai, xoa_mem, ngay_tao) VALUES
('dm1', 'DM001', 'Giày Chạy Bộ', 0, 0, 1711814400000),
('dm2', 'DM002', 'Giày Thể Thao', 0, 0, 1711814400000),
('dm3', 'DM003', 'Giày Bóng Đá', 0, 0, 1711814400000),
('dm4', 'DM004', 'Giày Sneaker', 0, 0, 1711814400000),
('dm5', 'DM005', 'Giày Lười', 0, 0, 1711814400000);

-- 7. Bảng cỡ Giày (co_giay)
INSERT INTO co_giay (id, ma_co_giay, ten_co_giay, trang_thai, xoa_mem, ngay_tao) VALUES
('cg1', 'CG001', 'Cổ Thấp', 0, 0, 1711814400000),
('cg2', 'CG002', 'Cổ Trung', 0, 0, 1711814400000),
('cg3', 'CG003', 'Cổ Cao', 0, 0, 1711814400000),
('cg4', 'CG004', 'Không Cổ', 0, 0, 1711814400000),
('cg5', 'CG005', 'Cổ Chun', 0, 0, 1711814400000);

-- 8. Bảng Chất Liệu (chat_lieu)
INSERT INTO chat_lieu (id, ma_chat_lieu, ten_chat_lieu, trang_thai, xoa_mem, ngay_tao) VALUES
('cl1', 'CL001', 'Da Thuộc', 0, 0, 1711814400000),
('cl2', 'CL002', 'Vải Mesh', 0, 0, 1711814400000),
('cl3', 'CL003', 'Vải Canvas', 0, 0, 1711814400000),
('cl4', 'CL004', 'Nhựa PVC', 0, 0, 1711814400000),
('cl5', 'CL005', 'Da Tổng Hợp', 0, 0, 1711814400000);

-- 9. Bảng Đế Giày (de_giay)
INSERT INTO de_giay (id, ma_de_giay, ten_de_giay, trang_thai, xoa_mem, ngay_tao) VALUES
('dg1', 'DG001', 'Đế Cao Su', 0, 0, 1711814400000),
('dg2', 'DG002', 'Đế Nhựa', 0, 0, 1711814400000),
('dg3', 'DG003', 'Đế Phíp', 0, 0, 1711814400000),
('dg4', 'DG004', 'Đế Kép', 0, 0, 1711814400000),
('dg5', 'DG005', 'Đế Eva', 0, 0, 1711814400000);

-- 10. Bảng Kích Thước (kich_thuoc)
INSERT INTO kich_thuoc (id, ma_kich_thuoc, ten_kich_thuoc, gia_tri_kich_thuoc, trang_thai, xoa_mem, ngay_tao) VALUES
('kt1', 'KT001', 'Size 39', '39', 0, 0, 1711814400000),
('kt2', 'KT002', 'Size 40', '40', 0, 0, 1711814400000),
('kt3', 'KT003', 'Size 41', '41', 0, 0, 1711814400000),
('kt4', 'KT004', 'Size 42', '42', 0, 0, 1711814400000),
('kt5', 'KT005', 'Size 43', '43', 0, 0, 1711814400000);

-- 11. Bảng Màu Sắc (mau_sac)
INSERT INTO mau_sac (id, ma_mau_sac, ten_mau_sac, ma_mau_hex, trang_thai, xoa_mem, ngay_tao) VALUES
('ms1', 'MS001', 'Đen', '#000000', 0, 0, 1711814400000),
('ms2', 'MS002', 'Trắng', '#FFFFFF', 0, 0, 1711814400000),
('ms3', 'MS003', 'Đỏ', '#FF0000', 0, 0, 1711814400000),
('ms4', 'MS004', 'Xanh Dương', '#0000FF', 0, 0, 1711814400000),
('ms5', 'MS005', 'Xám', '#808080', 0, 0, 1711814400000);

-- 12. Bảng Sản Phẩm (san_pham)
INSERT INTO san_pham (id, id_thuong_hieu, id_danh_muc, id_xuat_xu, id_muc_dich_chay, id_chat_lieu, id_de_giay, id_co_giay, ma_san_pham, ten_san_pham, gioi_tinh_khach_hang, hinh_anh, mo_ta_ngan, mo_ta_chi_tiet, trang_thai, xoa_mem, ngay_tao) VALUES
('sp1', 'th1', 'dm1', 'xx3', 'md3', 'cl1', 'dg1', 'cg1', 'SP001', 'Nike Air Max 2024', 'UNISEX', 'sp1.jpg', 'Dòng giày chạy bộ cao cấp Nike Air Max 2024.', 'Mô tả chi tiết giày Nike SP1', 0, 0, 1711814400000),
('sp2', 'th2', 'dm2', 'xx3', 'md1', 'cl2', 'dg2', 'cg2', 'SP002', 'Adidas UltraBoost', 'UNISEX', 'sp2.jpg', 'Trải nghiệm đỉnh cao với Adidas UltraBoost.', 'Mô tả chi tiết giày Adidas SP2', 0, 0, 1711814400000),
('sp3', 'th3', 'dm1', 'xx2', 'md2', 'cl3', 'dg3', 'cg3', 'SP003', 'Puma Speed', 'UNISEX', 'sp3.jpg', 'Giày chạy tốc độ Puma Speed đầy mạnh mẽ.', 'Mô tả chi tiết giày Puma SP3', 0, 0, 1711814400000),
('sp4', 'th4', 'dm4', 'xx2', 'md3', 'cl4', 'dg4', 'cg1', 'SP004', 'New Balance Classic', 'UNISEX', 'sp4.jpg', 'Phong cách cổ điển New Balance Classic.', 'Mô tả chi tiết giày NB SP4', 0, 0, 1711814400000),
('sp5', 'th5', 'dm4', 'xx1', 'md3', 'cl5', 'dg5', 'cg2', 'SP005', 'Converse All Star', 'UNISEX', 'sp5.jpg', 'Biểu tượng giới trẻ Converse All Star.', 'Mô tả chi tiết giày Converse SP5', 0, 0, 1711814400000);

-- 13. Bảng Chi Tiết Sản Phẩm (chi_tiet_san_pham)
INSERT INTO chi_tiet_san_pham (id, id_san_pham, id_kich_thuoc, id_mau_sac, ma_chi_tiet_san_pham, so_luong, gia_nhap, gia_ban, trang_thai, xoa_mem, ngay_tao) VALUES
('ct1', 'sp1', 'kt2', 'ms1', 'CT001', 100, 1000000, 1500000, 0, 0, 1711814400000),
('ct2', 'sp1', 'kt3', 'ms1', 'CT002', 50, 1000000, 1500000, 0, 0, 1711814400000),
('ct3', 'sp2', 'kt2', 'ms2', 'CT003', 80, 1200000, 1800000, 0, 0, 1711814400000),
('ct4', 'sp3', 'kt1', 'ms3', 'CT004', 30, 800000, 1200000, 0, 0, 1711814400000),
('ct5', 'sp4', 'kt4', 'ms4', 'CT005', 60, 1500000, 2200000, 0, 0, 1711814400000);

-- 14. Bảng Ảnh Chi Tiết Sản Phẩm (anh_chi_tiet_san_pham)
INSERT INTO anh_chi_tiet_san_pham (id, id_chi_tiet_san_pham, duong_dan_anh, hinh_anh_dai_dien, mo_ta, xoa_mem, trang_thai, ngay_tao) VALUES
('a1', 'ct1', 'anh1_1.jpg', 1, 'Ảnh mặt trước', 0, 0, 1711814400000),
('a2', 'ct1', 'anh1_2.jpg', 0, 'Ảnh mặt bên', 0, 0, 1711814400000),
('a3', 'ct2', 'anh2_1.jpg', 1, 'Ảnh đại diện ct2', 0, 0, 1711814400000),
('a4', 'ct3', 'anh3_1.jpg', 1, 'Ảnh đại diện ct3', 0, 0, 1711814400000),
('a5', 'ct4', 'anh4_1.jpg', 1, 'Ảnh đại diện ct4', 0, 0, 1711814400000);

-- 15. Bảng Đợt Giảm Giá (dot_giam_gia)
INSERT INTO dot_giam_gia (id, ma_dot_giam_gia, ten_dot_giam_gia, loai_giam_gia, so_tien_giam, dieu_kien_giam_gia, ngay_bat_dau, ngay_ket_thuc, muc_uu_tien, trang_thai, ngay_tao) VALUES
('dg1', 'DGG01', 'Sale Hè Rực Rỡ', 'PERCENTAGE', 20.00, 500000.00, 1711814400000, 1714492800000, 1, 0, 1711814400000),
('dg2', 'DGG02', 'Mừng Khai Trương', 'FIXED_AMOUNT', 100000.00, 1000000.00, 1711814400000, 1711900800000, 2, 0, 1711814400000),
('dg3', 'DGG03', 'Black Friday', 'PERCENTAGE' , 50.00, 0.00, 1732814400000, 1732900800000, 1, 0, 1711814400000),
('dg4', 'DGG04', 'Sale Thu Đông', 'PERCENTAGE', 15.00, 1000000.00, 1725120000000, 1730390400000, 3, 0, 1711814400000),
('dg5', 'DGG05', 'Xả Kho Cuối Năm', 'FIXED_AMOUNT', 200000.00, 500000.00, 1734201600000, 1735584000000, 1, 0, 1711814400000);

-- 16. Bảng Chi Tiết Đợt Giảm Giá (chi_tiet_dot_giam_gia)
INSERT INTO chi_tiet_dot_giam_gia (id, id_dot_giam_gia, id_chi_tiet_san_pham, gia_tri_giam, trang_thai, ngay_tao) VALUES
('ctdg1', 'dg1', 'ct1', 300000.00, 0, 1711814400000),
('ctdg2', 'dg1', 'ct2', 300000.00, 0, 1711814400000),
('ctdg3', 'dg2', 'ct3', 100000.00, 0, 1711814400000),
('ctdg4', 'dg1', 'ct4', 240000.00, 0, 1711814400000),
('ctdg5', 'dg1', 'ct5', 440000.00, 0, 1711814400000);

-- 17. Bảng Phiếu Giảm Giá (phieu_giam_gia)
INSERT INTO phieu_giam_gia (id, ma_phieu_giam_gia, ten_phieu, loai_phieu, phan_tram_giam_gia, so_tien_giam, so_luong, don_hang_toi_thieu, giam_toi_da, ngay_bat_dau, ngay_ket_thuc, ghi_chu, trang_thai, ngay_tao) VALUES
('pgg1', 'PGG01', 'Voucher Chào Mừng', 'PERCENTAGE', 10, NULL, 100, 0.00, 50000.00, 1711814400000, 1714492800000, 'Tặng khách mới', 0, 1711814400000),
('pgg2', 'PGG02', 'Giảm 50k Đơn 1 Triệu', 'FIXED_AMOUNT', NULL, 50000.00, 50, 1000000.00, 50000.00, 1711814400000, 1711900800000, NULL, 0, 1711814400000),
('pgg3', 'PGG03', 'Voucher VIP', 'PERCENTAGE', 20, NULL, 20, 2000000.00, 200000.00, 1711814400000, 1717171200000, NULL, 0, 1711814400000),
('pgg4', 'PGG04', 'Siêu Voucher', 'FIXED_AMOUNT', NULL, 500000.00, 10, 5000000.00, 500000.00, 1711814400000, 1711987200000, NULL, 0, 1711814400000),
('pgg5', 'PGG05', 'Voucher Sinh Nhật', 'PERCENTAGE', 30, NULL, 100, 0.00, 100000.00, 1711814400000, 1743350400000, NULL, 0, 1711814400000);

-- 18. Bảng Phiếu Giảm Giá Cá Nhân (phieu_giam_gia_ca_nhan)
INSERT INTO phieu_giam_gia_ca_nhan (id, id_khach_hang, id_phieu_giam_gia, ma_phieu_giam_gia_ca_nhan, ngay_nhan, da_su_dung, ngay_su_dung, xoa_mem, trang_thai, ngay_tao) VALUES
('pcn1', 'kh1', 'pgg1', 'PGG01_KH1', 1711814400000, 0, NULL, 0, 0, 1711814400000),
('pcn2', 'kh2', 'pgg1', 'PGG01_KH2', 1711814400000, 1, 1711850000000, 0, 0, 1711814400000),
('pcn3', 'kh3', 'pgg3', 'PGG03_KH3', 1711814400000, 0, NULL, 0, 0, 1711814400000),
('pcn4', 'kh4', 'pgg5', 'PGG05_KH4', 1711814400000, 0, NULL, 0, 0, 1711814400000),
('pcn5', 'kh5', 'pgg5', 'PGG05_KH5', 1711814400000, 0, NULL, 0, 0, 1711814400000);

-- 19. Bảng Hóa Đơn (hoa_don)
INSERT INTO hoa_don (id, id_phieu_giam_gia, id_phieu_giam_gia_ca_nhan, id_khach_hang, id_nhan_vien, ma_hoa_don, loai_don, phi_van_chuyen, tong_tien, tong_tien_sau_giam, tien_nguoi_mua, dia_chi_nguoi_nhan, so_dien_thoai_nguoi_nhan, ngay_du_kien_nhan, ghi_chu, trang_thai, ngay_tao) VALUES
('hd1', 'pgg1', NULL, 'kh1', 'nv1', 'HD001', 'OFFLINE', 0.00, 1500000.00, 1450000.00, 1450000.00, 'Tại cửa hàng', '0901112222', NULL, NULL, 3, 1711814400000),
('hd2', NULL, 'pcn2', 'kh2', 'nv2', 'HD002', 'ONLINE', 30000.00, 1800000.00, 1780000.00, 1780000.00, '45 Lê Lợi, Quận 1, TPHCM', '0902223333', 1712073600000, NULL, 1, 1711850000000),
('hd3', NULL, NULL, 'kh3', 'nv1', 'HD003', 'OFFLINE', 0.00, 1200000.00, 1200000.00, 1200000.00, 'Tại cửa hàng', '0903334444', NULL, NULL, 3, 1711860000000),
('hd4', 'pgg2', NULL, 'kh4', 'nv2', 'HD004', 'ONLINE', 0.00, 2200000.00, 2150000.00, 2150000.00, '90 Đinh Tiên Hoàng, Hải Phòng', '0904445555', 1712160000000, NULL, 0, 1711870000000),
('hd5', NULL, NULL, NULL, 'nv1', 'HD005', 'OFFLINE', 0.00, 1500000.00, 1500000.00, 1500000.00, 'Khách lẻ', NULL, NULL, NULL, 3, 1711880000000);

-- 20. Bảng Hóa Đơn Chi Tiết (hoa_don_chi_tiet)
INSERT INTO hoa_don_chi_tiet (id, id_hoa_don, id_chi_tiet_san_pham, ma_hoa_don_chi_tiet, so_luong, don_gia, trang_thai, ngay_tao) VALUES
('hdct1', 'hd1', 'ct1', 'HDCT001', 1, 1500000.00, 0, 1711814400000),
('hdct2', 'hd2', 'ct3', 'HDCT002', 1, 1800000.00, 0, 1711850000000),
('hdct3', 'hd3', 'ct4', 'HDCT003', 1, 1200000.00, 0, 1711860000000),
('hdct4', 'hd4', 'ct5', 'HDCT004', 1, 2200000.00, 0, 1711870000000),
('hdct5', 'hd5', 'ct1', 'HDCT005', 1, 1500000.00, 0, 1711880000000);

-- 21. Bảng Lịch Sử Trạng Thái Hóa Đơn (lich_su_trang_thai_hoa_don)
INSERT INTO lich_su_trang_thai_hoa_don (id, id_hoa_don, trang_thai_cu, trang_thai_moi, ghi_chu, nguoi_thuc_hien, thoi_gian) VALUES
('ls1', 'hd1', 0, 3, 'Hoàn thành thanh toán tại quầy', 'nv1', 1711815000000),
('ls2', 'hd2', 0, 1, 'Xác nhận đơn hàng', 'nv2', 1711851000000),
('ls3', 'hd3', 0, 3, 'Khách lấy hàng tại quầy', 'nv1', 1711861000000),
('ls4', 'hd4', NULL, 0, 'Khách đặt hàng online', 'kh4', 1711870000000),
('ls5', 'hd5', 0, 3, 'Bán cho khách lẻ', 'nv1', 1711881000000);

-- 22. Bảng Phương Thức Thanh Toán (phuong_thuc_thanh_toan)
INSERT INTO phuong_thuc_thanh_toan (id, ma_phuong_thuc_thanh_toan, ten_phuong_thuc_thanh_toan, trang_thai, ngay_tao) VALUES
('pt1', 'PT001', 'Tiền mặt', 0, 1711814400000),
('pt2', 'PT002', 'Chuyển khoản Ngân hàng', 0, 1711814400000),
('pt3', 'PT003', 'Ví điện tử MoMo', 0, 1711814400000),
('pt4', 'PT004', 'Ví điện tử ZaloPay', 0, 1711814400000),
('pt5', 'PT005', 'Thẻ tín dụng/Ghi nợ', 0, 1711814400000);

-- 23. Bảng Giao Dịch Thanh Toán (giao_dich_thanh_toan)
INSERT INTO giao_dich_thanh_toan (id, id_hoa_don, id_phuong_thuc_thanh_toan, ma_giao_dich_thanh_toan, so_tien, loai_giao_dich, ma_giao_dich_ngoai, ma_tham_chieu, duong_dan_thanh_toan, du_lieu_qr, thoi_gian_het_han, du_lieu_phan_hoi, thoi_gian_tao, trang_thai, ghi_chu) VALUES
('gd1', 'hd1', 'pt1', 'GD001', 1450000.00, 'PAYMENT', NULL, 'REF001', NULL, NULL, NULL, NULL, 1711814400000, 1, 'Thanh toán tiền mặt'),
('gd2', 'hd2', 'pt2', 'GD002', 1780000.00, 'PAYMENT', 'BANK123456', 'REF002', 'https://vnpay.vn/pay/hd2', 'QR_CODE_DATA', 1711853600000, 'SUCCESS', 1711850000000, 1, 'Chuyển khoản thành công'),
('gd3', 'hd3', 'pt1', 'GD003', 1200000.00, 'PAYMENT', NULL, 'REF003', NULL, NULL, NULL, NULL, 1711860000000, 1, 'Tiền mặt'),
('gd4', 'hd4', 'pt3', 'GD004', 2150000.00, 'PAYMENT', 'MOMO999', 'REF004', NULL, NULL, 1711873600000, NULL, 1711870000000, 0, 'Đang chờ MoMo'),
('gd5', 'hd5', 'pt1', 'GD005', 1500000.00, 'PAYMENT', NULL, 'REF005', NULL, NULL, NULL, NULL, 1711880000000, 1, 'Khách lẻ trả tiền mặt');
