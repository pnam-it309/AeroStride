-- Sửa DB cũ: các cột ảnh phải là LONGTEXT vì frontend có thể gửi ảnh base64 rất dài.
ALTER TABLE san_pham MODIFY COLUMN hinh_anh LONGTEXT;
ALTER TABLE anh_chi_tiet_san_pham MODIFY COLUMN duong_dan_anh LONGTEXT;
ALTER TABLE khach_hang MODIFY COLUMN hinh_anh LONGTEXT;
ALTER TABLE nhan_vien MODIFY COLUMN hinh_anh LONGTEXT;
