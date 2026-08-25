import dichVuFile from '@/services/core/dichVuFile';

// Hàm này trả về một URL hiển thị được cho thẻ <img>:
//  - data:/blob:/http(s): giữ nguyên
//  - đường dẫn tương đối (vd /uploads/..., uploads/..., file ID, path): tự động phân giải qua dichVuFile
export const getDisplayImageUrl = (value) => {
    if (!value || typeof value !== 'string') {
        return value || '';
    }
    return dichVuFile.layUrlFile(value);
};
