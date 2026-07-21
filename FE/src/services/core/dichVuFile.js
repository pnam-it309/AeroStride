import api from '../apiService';
import { API_COMMON, API_DEFAULTS } from '@/constants/apiPaths';

export const dichVuFile = {
    // Hàm nén ảnh trước khi lưu (giảm dung lượng base64 lưu vào DB)
    async nenAnh(file, maxWidth = 1600, maxHeight = 1600, quality = 0.8) {
        if (!file.type.startsWith('image/')) return file;

        return new Promise((resolve) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = (event) => {
                const img = new Image();
                img.src = event.target.result;
                img.onload = () => {
                    let { width, height } = img;
                    if (width > height && width > maxWidth) {
                        height = Math.round((height *= maxWidth / width));
                        width = maxWidth;
                    } else if (height > maxHeight) {
                        width = Math.round((width *= maxHeight / height));
                        height = maxHeight;
                    }

                    const canvas = document.createElement('canvas');
                    canvas.width = width;
                    canvas.height = height;
                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);

                    canvas.toBlob(
                        (blob) => {
                            if (!blob) return resolve(file);
                            const compressedFile = new File([blob], file.name, {
                                type: file.type === 'image/png' ? 'image/png' : 'image/jpeg',
                                lastModified: Date.now()
                            });
                            resolve(compressedFile.size < file.size ? compressedFile : file);
                        },
                        file.type === 'image/png' ? 'image/png' : 'image/jpeg',
                        quality
                    );
                };
                img.onerror = () => resolve(file);
            };
            reader.onerror = () => resolve(file);
        });
    },

    // Tải ảnh: nén rồi chuyển thành base64 data URL để lưu thẳng vào DB (đã bỏ Cloudinary).
    // Trả về chuỗi "data:image/...;base64,..." — dùng trực tiếp làm src ảnh và lưu vào cột ảnh.
    async taiLenFile(file) {
        const compressedFile = await this.nenAnh(file);
        return await this.fileToDataUrl(compressedFile);
    },

    // Chuyển File/Blob thành chuỗi base64 data URL
    fileToDataUrl(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = () => reject(new Error('Không đọc được tệp'));
            reader.readAsDataURL(file);
        });
    },

    // Lấy danh sách file
    async layDanhSachFile(params = {}) {
        const response = await api.get(`${API_COMMON.STORAGE}/files`, { params });
        return response.data.data;
    },

    // Xóa file
    async xoaFile(fileId) {
        const response = await api.delete(`${API_COMMON.STORAGE}/files/${fileId}`);
        return response.data;
    },

    // Lấy URL file
    layUrlFile(filePath) {
        const apiBase = import.meta.env.VITE_API_URL || API_DEFAULTS.PREFIX;
        return `${apiBase}${API_COMMON.STORAGE}/files/${filePath}`;
    }
};
