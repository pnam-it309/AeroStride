-- Migration to add indexes for AI search performance optimization
-- Target: Optimize keyword search on product names and price range filtering

-- Index for product name search (used in search filters)
CREATE INDEX idx_san_pham_ten_trang_thai ON san_pham (ten_san_pham, trang_thai, xoa_mem);

-- Index for product brand and other attributes
CREATE INDEX idx_san_pham_ids_attr ON san_pham (id_thuong_hieu, id_danh_muc, id_chat_lieu);

-- Index for price filtering and sorting in variants
CREATE INDEX idx_chi_tiet_san_pham_gia_ban ON chi_tiet_san_pham (gia_ban, trang_thai, xoa_mem);

-- Index for specific variant lookup (Mau Sac, Kich Thuoc)
CREATE INDEX idx_chi_tiet_san_pham_lookup ON chi_tiet_san_pham (id_san_pham, id_mau_sac, id_kich_thuoc);
