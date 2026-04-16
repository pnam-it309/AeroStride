-- ==============================================================================
-- V1: Full Initial Schema + Role System + Refresh Token + Product Module Alignment
-- ==============================================================================

-- Bảng Phân Quyền
CREATE TABLE phan_quyen (
    id VARCHAR(36) PRIMARY KEY,
    ma_phan_quyen VARCHAR(50) UNIQUE,
    ten_phan_quyen VARCHAR(100),
    quyen_han VARCHAR(255),
    mo_ta TEXT,
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

-- Bảng Nhân Viên (có vai_tro: NHAN_VIEN | QUAN_TRI_VIEN)
CREATE TABLE nhan_vien (
    id VARCHAR(36) PRIMARY KEY,
    id_phan_quyen VARCHAR(36),
    ma_nhan_vien VARCHAR(50) UNIQUE,
    ten_nhan_vien VARCHAR(255),
    email VARCHAR(100) UNIQUE,
    sdt VARCHAR(15),
    ngay_sinh DATE,
    gioi_tinh BIT,
    ten_tai_khoan VARCHAR(100) UNIQUE,
    mat_khau VARCHAR(255),
    hinh_anh VARCHAR(500),
    xoa_mem BIT DEFAULT 0,
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    FOREIGN KEY (id_phan_quyen) REFERENCES phan_quyen(id)
);

-- Bảng Khách Hàng (có vai_tro: KHACH_HANG)
CREATE TABLE khach_hang (
    id VARCHAR(36) PRIMARY KEY,
    id_dia_chi VARCHAR(36),
    ma_nguoi_dung VARCHAR(50) UNIQUE,
    ten_nguoi_dung VARCHAR(255),
    email VARCHAR(100) UNIQUE,
    ten_tai_khoan VARCHAR(100) UNIQUE,
    mat_khau VARCHAR(255),
    gioi_tinh BIT,
    sdt VARCHAR(15),
    ngay_sinh DATE,
    hinh_anh VARCHAR(500),
    ghi_chu TEXT,
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);


-- Bảng Địa Chỉ
CREATE TABLE dia_chi (
    id VARCHAR(36) PRIMARY KEY,
    id_khach_hang VARCHAR(40),
    ma_dia_chi VARCHAR(50) UNIQUE,
    tinh VARCHAR(100),
    thanh_pho VARCHAR(100),
    phuong_xa VARCHAR(100),
    dia_chi_chi_tiet VARCHAR(255),
    ten_nguoi_nhan VARCHAR(255),
    sdt_nguoi_nhan VARCHAR(15),
    trang_thai INT,
    la_mac_dinh BIT DEFAULT b'0',
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    CONSTRAINT fk_dc_kh
     FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id)
);

ALTER TABLE khach_hang
    ADD CONSTRAINT fk_kh_dc
        FOREIGN KEY (id_dia_chi) REFERENCES dia_chi(id);

-- Bảng Refresh Token (liên kết với khach_hang hoặc nhan_vien)
CREATE TABLE refresh_token (
    id VARCHAR(36) PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    id_khach_hang VARCHAR(36),
    id_nhan_vien VARCHAR(36),
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id) ON DELETE CASCADE,
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id) ON DELETE CASCADE
);

-- Các bảng thuộc tính sản phẩm
CREATE TABLE xuat_xu (
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

CREATE TABLE muc_dich_chay (
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

CREATE TABLE thuong_hieu (
    id VARCHAR(36) PRIMARY KEY,
    ma_thuong_hieu VARCHAR(50) UNIQUE,
    ten_thuong_hieu VARCHAR(255),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

CREATE TABLE danh_muc (
    id VARCHAR(36) PRIMARY KEY,
    ma_danh_muc VARCHAR(50) UNIQUE,
    ten_danh_muc VARCHAR(255),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

CREATE TABLE co_giay (
    id VARCHAR(36) PRIMARY KEY,
    ma_co_giay VARCHAR(50) UNIQUE,
    ten_co_giay VARCHAR(255),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

CREATE TABLE chat_lieu (
    id VARCHAR(36) PRIMARY KEY,
    ma_chat_lieu VARCHAR(50) UNIQUE,
    ten_chat_lieu VARCHAR(255),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

CREATE TABLE de_giay (
    id VARCHAR(36) PRIMARY KEY,
    ma_de_giay VARCHAR(50) UNIQUE,
    ten_de_giay VARCHAR(255),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

CREATE TABLE kich_thuoc (
    id VARCHAR(36) PRIMARY KEY,
    ma_kich_thuoc VARCHAR(50) UNIQUE,
    ten_kich_thuoc VARCHAR(255),
    gia_tri_kich_thuoc VARCHAR(50),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

CREATE TABLE mau_sac (
    id VARCHAR(36) PRIMARY KEY,
    ma_mau_sac VARCHAR(50) UNIQUE,
    ten_mau_sac VARCHAR(255),
    ma_mau_hex VARCHAR(10),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

-- Bảng Sản Phẩm
CREATE TABLE san_pham (
    id VARCHAR(36) PRIMARY KEY,
    id_thuong_hieu VARCHAR(36),
    id_danh_muc VARCHAR(36),
    id_xuat_xu VARCHAR(36),
    id_muc_dich_chay VARCHAR(36),
    id_chat_lieu VARCHAR(36),
    id_de_giay VARCHAR(36),
    id_co_giay VARCHAR(36),
    ma_san_pham VARCHAR(50) UNIQUE,
    ten_san_pham VARCHAR(255),
    gioi_tinh_khach_hang VARCHAR(20) NOT NULL,
    hinh_anh VARCHAR(255),
    mo_ta_ngan TEXT,
    mo_ta_chi_tiet TEXT,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    FOREIGN KEY (id_thuong_hieu) REFERENCES thuong_hieu(id),
    FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id),
    FOREIGN KEY (id_xuat_xu) REFERENCES xuat_xu(id),
    FOREIGN KEY (id_muc_dich_chay) REFERENCES muc_dich_chay(id),
    FOREIGN KEY (id_chat_lieu) REFERENCES chat_lieu(id),
    FOREIGN KEY (id_de_giay) REFERENCES de_giay(id),
    FOREIGN KEY (id_co_giay) REFERENCES co_giay(id)
);

-- Bảng Chi Tiết Sản Phẩm
CREATE TABLE chi_tiet_san_pham (
    id VARCHAR(36) PRIMARY KEY,
    id_san_pham VARCHAR(36),
    id_kich_thuoc VARCHAR(36),
    id_mau_sac VARCHAR(36),
    ma_chi_tiet_san_pham VARCHAR(50) UNIQUE,
    so_luong INT,
    gia_nhap DECIMAL(20,2),
    gia_ban DECIMAL(20,2),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    FOREIGN KEY (id_san_pham) REFERENCES san_pham(id),
    FOREIGN KEY (id_kich_thuoc) REFERENCES kich_thuoc(id),
    FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id),
    CONSTRAINT uk_ctsp_san_pham_mau_kich_thuoc_xoa_mem UNIQUE (id_san_pham, id_mau_sac, id_kich_thuoc, xoa_mem)
);

-- Bảng Ảnh Chi Tiết Sản Phẩm
CREATE TABLE anh_chi_tiet_san_pham (
    id VARCHAR(36) PRIMARY KEY,
    id_chi_tiet_san_pham VARCHAR(36),
    duong_dan_anh VARCHAR(500),
    hinh_anh_dai_dien BIT,
    mo_ta TEXT,
    xoa_mem BIT DEFAULT 0,
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id)
);

-- Bảng Đợt Giảm Giá
CREATE TABLE dot_giam_gia (
    id VARCHAR(36) PRIMARY KEY,
    ma_dot_giam_gia VARCHAR(50) UNIQUE,
    ten_dot_giam_gia VARCHAR(255),
    loai_giam_gia VARCHAR(50),
    so_tien_giam DECIMAL(20,2),
    dieu_kien_giam_gia DECIMAL(20,2),
    ngay_bat_dau BIGINT,
    ngay_ket_thuc BIGINT,
    muc_uu_tien INT,
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

-- Bảng Chi Tiết Đợt Giảm Giá
CREATE TABLE chi_tiet_dot_giam_gia (
    id VARCHAR(36) PRIMARY KEY,
    id_dot_giam_gia VARCHAR(36),
    id_chi_tiet_san_pham VARCHAR(36),
    gia_tri_giam DECIMAL(20,2),
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    FOREIGN KEY (id_dot_giam_gia) REFERENCES dot_giam_gia(id),
    FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id)
);

-- Bảng Phiếu Giảm Giá
CREATE TABLE phieu_giam_gia (
    id VARCHAR(36) PRIMARY KEY,
    ma_phieu_giam_gia VARCHAR(50) UNIQUE,
    ten_phieu VARCHAR(255),
    loai_phieu VARCHAR(50),
    phan_tram_giam_gia INT,
    so_tien_giam DECIMAL(20,2),
    so_luong INT,
    don_hang_toi_thieu DECIMAL(20,2),
    giam_toi_da DECIMAL(20,2),
    ngay_bat_dau BIGINT,
    ngay_ket_thuc BIGINT,
    ghi_chu TEXT,
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

-- Bảng Phiếu Giảm Giá Cá Nhân
CREATE TABLE phieu_giam_gia_ca_nhan (
    id VARCHAR(36) PRIMARY KEY,
    id_khach_hang VARCHAR(36),
    id_phieu_giam_gia VARCHAR(36),
    ma_phieu_giam_gia_ca_nhan VARCHAR(50) UNIQUE,
    ngay_nhan BIGINT,
    da_su_dung BIT,
    ngay_su_dung BIGINT,
    xoa_mem BIT DEFAULT 0,
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id)
);

-- Bảng Hóa Đơn
CREATE TABLE hoa_don (
    id VARCHAR(36) PRIMARY KEY,
    id_phieu_giam_gia VARCHAR(36),
    id_phieu_giam_gia_ca_nhan VARCHAR(36),
    id_khach_hang VARCHAR(36),
    id_nhan_vien VARCHAR(36),
    ma_hoa_don VARCHAR(50) UNIQUE,
    loai_don VARCHAR(50),
    phi_van_chuyen DECIMAL(20,2),
    tong_tien DECIMAL(20,2),
    tong_tien_sau_giam DECIMAL(20,2),
    tien_nguoi_mua DECIMAL(20,2),
    dia_chi_nguoi_nhan VARCHAR(500),
    so_dien_thoai_nguoi_nhan VARCHAR(15),
    ngay_du_kien_nhan BIGINT,
    ghi_chu TEXT,
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id),
    FOREIGN KEY (id_phieu_giam_gia_ca_nhan) REFERENCES phieu_giam_gia_ca_nhan(id),
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
);

-- Bảng Hóa Đơn Chi Tiết
CREATE TABLE hoa_don_chi_tiet (
    id VARCHAR(36) PRIMARY KEY,
    id_hoa_don VARCHAR(36),
    id_chi_tiet_san_pham VARCHAR(36),
    ma_hoa_don_chi_tiet VARCHAR(50) UNIQUE,
    so_luong INT,
    don_gia DECIMAL(20,2),
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100),
    FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id)
);

-- Bảng Lịch Sử Trạng Thái Hóa Đơn
CREATE TABLE lich_su_trang_thai_hoa_don (
    id VARCHAR(36) PRIMARY KEY,
    id_hoa_don VARCHAR(36),
    trang_thai_cu INT,
    trang_thai_moi INT,
    ghi_chu TEXT,
    nguoi_thuc_hien VARCHAR(100),
    thoi_gian BIGINT,
    FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
);

-- Bảng Phương Thức Thanh Toán
CREATE TABLE phuong_thuc_thanh_toan (
    id VARCHAR(36) PRIMARY KEY,
    ma_phuong_thuc_thanh_toan VARCHAR(50) UNIQUE,
    ten_phuong_thuc_thanh_toan VARCHAR(255),
    trang_thai INT,
    ngay_tao BIGINT,
    ngay_cap_nhat BIGINT,
    nguoi_tao VARCHAR(100),
    nguoi_cap_nhat VARCHAR(100)
);

-- Bảng Giao Dịch Thanh Toán
CREATE TABLE giao_dich_thanh_toan (
    id VARCHAR(36) PRIMARY KEY,
    id_hoa_don VARCHAR(36),
    id_phuong_thuc_thanh_toan VARCHAR(36),
    ma_giao_dich_thanh_toan VARCHAR(50) UNIQUE,
    so_tien DECIMAL(20,2),
    loai_giao_dich VARCHAR(50),
    ma_giao_dich_ngoai VARCHAR(100),
    ma_tham_chieu VARCHAR(100),
    duong_dan_thanh_toan VARCHAR(1000),
    du_lieu_qr TEXT,
    thoi_gian_het_han BIGINT,
    du_lieu_phan_hoi TEXT,
    thoi_gian_tao BIGINT,
    thoi_gian_cap_nhat BIGINT,
    trang_thai INT,
    ghi_chu TEXT,
    FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);
