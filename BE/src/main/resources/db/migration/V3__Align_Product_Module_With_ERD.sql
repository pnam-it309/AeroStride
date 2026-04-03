-- Align product module with ERD and backend admin requirements

CREATE TABLE IF NOT EXISTS xuat_xu (
    id VARCHAR(36) PRIMARY KEY,
    ma_xuat_xu VARCHAR(50) UNIQUE,
    ten_xuat_xu VARCHAR(255),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS muc_dich_chay (
    id VARCHAR(36) PRIMARY KEY,
    ma_muc_dich_chay VARCHAR(50) UNIQUE,
    ten_muc_dich_chay VARCHAR(255),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

ALTER TABLE san_pham
    ADD COLUMN id_xuat_xu VARCHAR(36) NULL AFTER id_thuong_hieu,
    ADD COLUMN id_muc_dich_chay VARCHAR(36) NULL AFTER id_xuat_xu,
    ADD COLUMN gioi_tinh_khach_hang VARCHAR(20) NULL AFTER ten_san_pham,
    ADD COLUMN mo_ta_ngan TEXT NULL AFTER gioi_tinh_khach_hang;

ALTER TABLE san_pham
    CHANGE COLUMN anh_chinh hinh_anh VARCHAR(255);

UPDATE san_pham
SET gioi_tinh_khach_hang = COALESCE(gioi_tinh_khach_hang, 'UNISEX'),
    mo_ta_ngan = COALESCE(mo_ta_ngan, LEFT(mo_ta_chi_tiet, 255)),
    xoa_mem = COALESCE(xoa_mem, 0);

UPDATE chi_tiet_san_pham
SET xoa_mem = COALESCE(xoa_mem, 0);

UPDATE anh_chi_tiet_san_pham
SET xoa_mem = COALESCE(xoa_mem, 0);

INSERT INTO xuat_xu (id, ma_xuat_xu, ten_xuat_xu, trang_thai, xoa_mem, ngay_tao)
SELECT 'xx1', 'XX001', 'Việt Nam', 0, 0, 1711814400000
WHERE NOT EXISTS (SELECT 1 FROM xuat_xu WHERE id = 'xx1');

INSERT INTO xuat_xu (id, ma_xuat_xu, ten_xuat_xu, trang_thai, xoa_mem, ngay_tao)
SELECT 'xx2', 'XX002', 'Trung Quốc', 0, 0, 1711814400000
WHERE NOT EXISTS (SELECT 1 FROM xuat_xu WHERE id = 'xx2');

INSERT INTO xuat_xu (id, ma_xuat_xu, ten_xuat_xu, trang_thai, xoa_mem, ngay_tao)
SELECT 'xx3', 'XX003', 'Mỹ', 0, 0, 1711814400000
WHERE NOT EXISTS (SELECT 1 FROM xuat_xu WHERE id = 'xx3');

INSERT INTO muc_dich_chay (id, ma_muc_dich_chay, ten_muc_dich_chay, trang_thai, xoa_mem, ngay_tao)
SELECT 'md1', 'MD001', 'Chạy Đường Dài', 0, 0, 1711814400000
WHERE NOT EXISTS (SELECT 1 FROM muc_dich_chay WHERE id = 'md1');

INSERT INTO muc_dich_chay (id, ma_muc_dich_chay, ten_muc_dich_chay, trang_thai, xoa_mem, ngay_tao)
SELECT 'md2', 'MD002', 'Chạy Tốc Độ', 0, 0, 1711814400000
WHERE NOT EXISTS (SELECT 1 FROM muc_dich_chay WHERE id = 'md2');

INSERT INTO muc_dich_chay (id, ma_muc_dich_chay, ten_muc_dich_chay, trang_thai, xoa_mem, ngay_tao)
SELECT 'md3', 'MD003', 'Luyện Tập Hàng Ngày', 0, 0, 1711814400000
WHERE NOT EXISTS (SELECT 1 FROM muc_dich_chay WHERE id = 'md3');

UPDATE san_pham
SET id_xuat_xu = CASE id
        WHEN 'sp1' THEN 'xx3'
        WHEN 'sp2' THEN 'xx3'
        WHEN 'sp3' THEN 'xx2'
        WHEN 'sp4' THEN 'xx2'
        WHEN 'sp5' THEN 'xx1'
        ELSE id_xuat_xu
    END,
    id_muc_dich_chay = CASE id
        WHEN 'sp1' THEN 'md3'
        WHEN 'sp2' THEN 'md1'
        WHEN 'sp3' THEN 'md2'
        WHEN 'sp4' THEN 'md3'
        WHEN 'sp5' THEN 'md3'
        ELSE id_muc_dich_chay
    END;

ALTER TABLE san_pham
    MODIFY COLUMN gioi_tinh_khach_hang VARCHAR(20) NOT NULL,
    ADD CONSTRAINT fk_san_pham_xuat_xu FOREIGN KEY (id_xuat_xu) REFERENCES xuat_xu(id),
    ADD CONSTRAINT fk_san_pham_muc_dich_chay FOREIGN KEY (id_muc_dich_chay) REFERENCES muc_dich_chay(id);

ALTER TABLE san_pham
    DROP FOREIGN KEY san_pham_ibfk_3;

ALTER TABLE san_pham
    DROP COLUMN id_mau_sac;

ALTER TABLE chi_tiet_san_pham
    ADD CONSTRAINT uk_ctsp_san_pham_mau_kich_thuoc_xoa_mem UNIQUE (id_san_pham, id_mau_sac, id_kich_thuoc, xoa_mem);
