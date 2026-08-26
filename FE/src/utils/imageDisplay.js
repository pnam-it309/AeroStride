import dichVuFile from '@/services/core/dichVuFile';

/**
 * Tối ưu URL hình ảnh (Unsplash, Cloudinary, etc.) để tải cực nhanh với kích thước & chất lượng phù hợp
 */
export const optimizeImageUrl = (url, width = 800, quality = 75) => {
    if (!url || typeof url !== 'string') return url || '';
    const clean = url.trim();

    // Nếu là base64 hoặc local blob -> giữ nguyên
    if (clean.startsWith('data:') || clean.startsWith('blob:')) return clean;

    // 1. Tối ưu Unsplash CDN: tự động chỉnh w, q, auto=format, fit=crop (giảm từ 4MB xuống ~40KB)
    if (clean.includes('images.unsplash.com')) {
        try {
            const parsed = new URL(clean);
            parsed.searchParams.set('w', String(width));
            parsed.searchParams.set('q', String(quality));
            parsed.searchParams.set('auto', 'format');
            parsed.searchParams.set('fit', 'crop');
            return parsed.toString();
        } catch (e) {
            return clean.replace(/w=\d+/, `w=${width}`).replace(/q=\d+/, `q=${quality}`);
        }
    }

    // 2. Tối ưu Cloudinary CDN
    if (clean.includes('res.cloudinary.com') && clean.includes('/upload/')) {
        if (!clean.includes('/upload/f_auto,q_auto')) {
            return clean.replace('/upload/', `/upload/f_auto,q_auto,w_${width}/`);
        }
    }

    return clean;
};

// Hàm này trả về một URL hiển thị được cho thẻ <img>:
//  - data:/blob:/http(s): giữ nguyên và tối ưu CDN
//  - đường dẫn tương đối (vd /uploads/..., uploads/..., file ID, path): tự động phân giải qua dichVuFile
export const getDisplayImageUrl = (value, width = 800, quality = 75) => {
    if (!value || typeof value !== 'string') {
        return value || '';
    }
    const resolved = dichVuFile.layUrlFile(value);
    return optimizeImageUrl(resolved, width, quality);
};
