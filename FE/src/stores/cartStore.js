import { defineStore } from 'pinia';

const CART_STORAGE_KEY = 'aerostride_cart';
const CART_VERSION = 2; // Tăng version khi thay đổi cấu trúc cart
const CART_VERSION_KEY = 'aerostride_cart_version';

/**
 * Kiểm tra và migrate cart data nếu version cũ hơn.
 * Version 1: chỉ lưu {idChiTietSanPham, soLuong}
 * Version 2: lưu đầy đủ metadata {tenSanPham, hinhAnh, tenMauSac, ...}
 */
function loadCartFromStorage() {
    const savedVersion = parseInt(localStorage.getItem(CART_VERSION_KEY) || '1', 10);
    if (savedVersion < CART_VERSION) {
        // Clear stale cart data khi đổi cấu trúc
        localStorage.removeItem(CART_STORAGE_KEY);
        localStorage.setItem(CART_VERSION_KEY, String(CART_VERSION));
        return [];
    }
    return JSON.parse(localStorage.getItem(CART_STORAGE_KEY)) || [];
}

export const useCartStore = defineStore('cart', {
    state: () => ({
        items: loadCartFromStorage(),
        serverTotal: 0,
        isDrawerOpen: false,
        isSyncing: false
    }),

    getters: {
        cartCount: (state) => state.items.reduce((sum, item) => sum + item.soLuong, 0),
        // Ưu tiên tổng tiền từ server (đã tính giảm giá); nếu chưa có thì tính tạm từ metadata cục bộ
        cartTotal: (state) => {
            if (state.serverTotal > 0) return state.serverTotal;
            return state.items.reduce((sum, item) => {
                const price = item.giaBan || 0;
                return sum + price * item.soLuong;
            }, 0);
        },
        cartItems: (state) => state.items,
        isEmpty: (state) => state.items.length === 0
    },

    actions: {
        /**
         * Lưu toàn bộ thông tin item vào localStorage (bao gồm metadata hiển thị)
         * để drawer có thể hiển thị ngay khi tải lại trang, trước khi sync hoàn tất.
         */
        _persist() {
            localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(this.items.map(i => ({
                idChiTietSanPham: i.idChiTietSanPham,
                soLuong: i.soLuong,
                // Lưu metadata để hiển thị ngay khi reload (trước khi sync hoàn tất)
                tenSanPham: i.tenSanPham || null,
                hinhAnh: i.hinhAnh || null,
                tenMauSac: i.tenMauSac || null,
                tenKichThuoc: i.tenKichThuoc || null,
                giaBan: i.giaBan || null,
                soLuongTonKho: i.soLuongTonKho || null
            }))));
        },

        async syncWithBackend() {
            if (this.items.length === 0) {
                this.serverTotal = 0;
                return { success: true };
            }
            this.isSyncing = true;
            try {
                const { dichVuCart } = await import('@/services/public/dichVuCart');
                const result = await dichVuCart.syncCart(this.items);
                if (result) {
                    this.items = result.items;
                    this.serverTotal = result.tongTien;
                    // Sau khi sync, lưu lại localStorage với đầy đủ metadata từ server
                    this._persist();

                    const failedItems = result.items.filter(i => !i.isAvailable);
                    if (failedItems.length > 0) {
                        return { success: false, message: failedItems[0].message };
                    }
                }
            } catch (e) {
                console.error("Cart sync failed", e);
            } finally {
                this.isSyncing = false;
            }
            return { success: true };
        },

        /**
         * Thêm sản phẩm vào giỏ hàng.
         * @param {Object} product - Thông tin sản phẩm, bao gồm idChiTietSanPham, soLuong,
         *   và metadata hiển thị tùy chọn: tenSanPham, hinhAnh, tenMauSac, tenKichThuoc, giaBan
         */
        async addToCart(product) {
            const existing = this.items.find((i) => i.idChiTietSanPham === product.idChiTietSanPham);
            if (existing) {
                existing.soLuong += (product.soLuong || 1);
                // Cập nhật metadata nếu được cung cấp
                if (product.tenSanPham) existing.tenSanPham = product.tenSanPham;
                if (product.hinhAnh) existing.hinhAnh = product.hinhAnh;
                if (product.tenMauSac) existing.tenMauSac = product.tenMauSac;
                if (product.tenKichThuoc) existing.tenKichThuoc = product.tenKichThuoc;
                if (product.giaBan) existing.giaBan = product.giaBan;
            } else {
                this.items.push({
                    idChiTietSanPham: product.idChiTietSanPham,
                    soLuong: product.soLuong || 1,
                    // Lưu metadata tạm thời để hiển thị ngay trong drawer
                    tenSanPham: product.tenSanPham || null,
                    hinhAnh: product.hinhAnh || null,
                    tenMauSac: product.tenMauSac || null,
                    tenKichThuoc: product.tenKichThuoc || null,
                    giaBan: product.giaBan || null,
                    soLuongTonKho: product.soLuongTonKho || null
                });
            }
            this._persist();
            const syncResult = await this.syncWithBackend();
            if (!syncResult.success) {
                // Nếu backend từ chối (hết hàng...), rollback
                if (existing) {
                    existing.soLuong -= (product.soLuong || 1);
                } else {
                    this.items = this.items.filter(i => i.idChiTietSanPham !== product.idChiTietSanPham);
                }
                this._persist();
                await this.syncWithBackend();
                return syncResult;
            }
            return { success: true, message: 'Đã thêm vào giỏ hàng' };
        },

        async removeFromCart(idChiTietSanPham) {
            this.items = this.items.filter((i) => i.idChiTietSanPham !== idChiTietSanPham);
            this._persist();
            await this.syncWithBackend();
        },

        async updateQuantity(idChiTietSanPham, soLuong) {
            if (soLuong <= 0) {
                return this.removeFromCart(idChiTietSanPham);
            }
            const item = this.items.find((i) => i.idChiTietSanPham === idChiTietSanPham);
            if (item) {
                const oldQty = item.soLuong;
                item.soLuong = soLuong;
                this._persist();

                const syncResult = await this.syncWithBackend();
                if (!syncResult.success) {
                    item.soLuong = oldQty;
                    this._persist();
                    await this.syncWithBackend();
                    return syncResult;
                }
            }
            return { success: true };
        },

        clearCart() {
            this.items = [];
            this.serverTotal = 0;
            this._persist();
        },

        openDrawer() {
            this.isDrawerOpen = true;
            this.syncWithBackend();
        },

        closeDrawer() {
            this.isDrawerOpen = false;
        },

        toggleDrawer() {
            this.isDrawerOpen = !this.isDrawerOpen;
            if (this.isDrawerOpen) {
                this.syncWithBackend();
            }
        }
    }
});
