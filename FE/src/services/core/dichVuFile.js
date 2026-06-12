import api from '../apiService';
import { API_COMMON, API_DEFAULTS } from '@/constants/apiPaths';

export const dichVuFile = {
    // Hàm nén ảnh trước khi upload (giúp tối ưu Cloudinary load nhanh gấp 10 lần)
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

    // Tải lên file
    async taiLenFile(file) {
        // Nén ảnh trước khi gửi (giảm dung lượng từ MB xuống vài chục KB)
        const compressedFile = await this.nenAnh(file);

        const formData = new FormData();
        formData.append('file', compressedFile);

        const response = await api.post(`${API_COMMON.STORAGE}/upload`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        return response.data.data;
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
