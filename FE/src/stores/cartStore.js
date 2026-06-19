import { defineStore } from 'pinia';

const CART_STORAGE_KEY = 'aerostride_cart';

/**
 * Module: Giỏ hàng (Cart Store)
 * Chức năng: Quản lý trạng thái giỏ hàng của khách hàng trên giao diện, bao gồm thêm, sửa, xóa,
 * tính tổng tiền, và lưu trữ cục bộ (localStorage) để không bị mất dữ liệu khi tải lại trang.
 */
export const useCartStore = defineStore('cart', {
    state: () => ({
        items: JSON.parse(localStorage.getItem(CART_STORAGE_KEY)) || [],
        serverTotal: 0,
        isDrawerOpen: false
    }),

    getters: {
        cartCount: (state) => state.items.reduce((sum, item) => sum + item.soLuong, 0),
        cartTotal: (state) => state.serverTotal,
        cartItems: (state) => state.items,
        isEmpty: (state) => state.items.length === 0
    },

    actions: {
        _persist() {
            localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(this.items.map(i => ({
                idChiTietSanPham: i.idChiTietSanPham,
                soLuong: i.soLuong
            }))));
        },

        async syncWithBackend() {
            try {
                const { dichVuCart } = await import('@/services/public/dichVuCart');
                const result = await dichVuCart.syncCart(this.items);
                if (result) {
                    this.items = result.items;
                    this.serverTotal = result.tongTien;
                    
                    const failedItems = result.items.filter(i => !i.isAvailable);
                    if (failedItems.length > 0) {
                        return { success: false, message: failedItems[0].message };
                    }
                }
            } catch (e) {
                console.error("Cart sync failed", e);
            }
            return { success: true };
        },

        async addToCart(product) {
            const existing = this.items.find((i) => i.idChiTietSanPham === product.idChiTietSanPham);
            if (existing) {
                existing.soLuong += (product.soLuong || 1);
            } else {
                this.items.push({
                    idChiTietSanPham: product.idChiTietSanPham,
                    soLuong: product.soLuong || 1
                });
            }
            this._persist();
            const syncResult = await this.syncWithBackend();
            if (!syncResult.success) {
                // If backend rejects (e.g. out of stock), rollback or show error
                if (existing) {
                    existing.soLuong -= (product.soLuong || 1);
                } else {
                    this.items.pop();
                }
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
        },

        closeDrawer() {
            this.isDrawerOpen = false;
        },

        toggleDrawer() {
            this.isDrawerOpen = !this.isDrawerOpen;
        }
    }
});
