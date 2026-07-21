// Ứng dụng đã bỏ Cloudinary. Ảnh mới được lưu base64 (data URL) trực tiếp trong DB.
// Hàm này trả về một URL hiển thị được cho thẻ <img>:
//  - data:/blob:/http(s):  giữ nguyên
//  - đường dẫn tương đối (vd /uploads/...): giữ nguyên để trình duyệt tự phân giải theo origin
export const getDisplayImageUrl = (value) => {
    if (!value || typeof value !== 'string') {
        return value || '';
    }
    return value;
};
