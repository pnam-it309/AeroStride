-- Sửa lỗi thêm sản phẩm/biến thể khi ảnh base64 dài hơn độ dài cột hiện tại trong DB.
-- V1 đã định nghĩa LONGTEXT, nhưng các DB đã tạo từ schema cũ/baseline cũ vẫn có thể còn VARCHAR.
ALTER TABLE san_pham
    MODIFY COLUMN hinh_anh LONGTEXT;

ALTER TABLE anh_chi_tiet_san_pham
    MODIFY COLUMN duong_dan_anh LONGTEXT;
