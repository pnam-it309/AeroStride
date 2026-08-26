import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

/**
 * Lấy storage key cho danh sách yêu thích dựa theo tài khoản đang đăng nhập.
 * - Khách chưa đăng nhập: 'aerostride_favorites_guest'
 * - Khách đã đăng nhập: 'aerostride_favorites_<username>'
 */
export function getFavoritesStorageKey() {
    try {
        const user = dichVuXacThuc.layUserHienTai();
        if (user && user.username) {
            return `aerostride_favorites_${user.username}`;
        }
    } catch (e) {
        console.error('Lỗi khi lấy thông tin user cho favorites key:', e);
    }
    return 'aerostride_favorites_guest';
}

/**
 * Lấy danh sách ID sản phẩm yêu thích của tài khoản hiện tại
 */
export function getFavoriteIds() {
    try {
        const key = getFavoritesStorageKey();
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : [];
    } catch (e) {
        console.error('Lỗi khi đọc danh sách favorites:', e);
        return [];
    }
}

/**
 * Lưu danh sách ID sản phẩm yêu thích cho tài khoản hiện tại và bắn event cập nhật
 */
export function setFavoriteIds(ids) {
    try {
        const key = getFavoritesStorageKey();
        localStorage.setItem(key, JSON.stringify(ids || []));
        notifyFavoritesUpdated();
    } catch (e) {
        console.error('Lỗi khi lưu danh sách favorites:', e);
    }
}

/**
 * Thêm hoặc gỡ sản phẩm khỏi danh sách yêu thích
 */
export function toggleFavorite(productId) {
    if (!productId) return getFavoriteIds();
    let ids = getFavoriteIds();
    if (ids.includes(productId)) {
        ids = ids.filter((id) => id !== productId);
    } else {
        ids.push(productId);
    }
    setFavoriteIds(ids);
    return ids;
}

/**
 * Gỡ sản phẩm khỏi danh sách yêu thích
 */
export function removeFavorite(productId) {
    if (!productId) return getFavoriteIds();
    let ids = getFavoriteIds();
    ids = ids.filter((id) => id !== productId);
    setFavoriteIds(ids);
    return ids;
}

/**
 * Kiểm tra xem sản phẩm có nằm trong danh sách yêu thích không
 */
export function isFavorite(productId) {
    if (!productId) return false;
    const ids = getFavoriteIds();
    return ids.includes(productId);
}

/**
 * Bắn event cập nhật danh sách yêu thích toàn app
 */
export function notifyFavoritesUpdated() {
    window.dispatchEvent(new Event('favorites-updated'));
}
