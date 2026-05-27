import { defineStore } from 'pinia';

const CART_STORAGE_KEY = 'aerostride_cart';

export const useCartStore = defineStore('cart', {
    state: () => ({
        items: JSON.parse(localStorage.getItem(CART_STORAGE_KEY)) || [],
        isDrawerOpen: false
    }),

    getters: {
        cartCount: (state) => state.items.reduce((sum, item) => sum + item.soLuong, 0),

        cartTotal: (state) => state.items.reduce((sum, item) => sum + item.giaBan * item.soLuong, 0),

        cartItems: (state) => state.items,

        isEmpty: (state) => state.items.length === 0
    },

    actions: {
        _persist() {
            localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(this.items));
        },

        addToCart(product) {
            // product: { idChiTietSanPham, tenSanPham, hinhAnh, tenMauSac, tenKichThuoc, giaBan, soLuong, soLuongTonKho }
            const existing = this.items.find((i) => i.idChiTietSanPham === product.idChiTietSanPham);

            if (existing) {
                const newQty = existing.soLuong + (product.soLuong || 1);
                if (newQty > (product.soLuongTonKho || 99)) {
                    return { success: false, message: `Chỉ còn ${product.soLuongTonKho} sản phẩm trong kho` };
                }
                existing.soLuong = newQty;
            } else {
                this.items.push({
                    idChiTietSanPham: product.idChiTietSanPham,
                    tenSanPham: product.tenSanPham,
                    hinhAnh: product.hinhAnh || '',
                    tenMauSac: product.tenMauSac || '',
                    tenKichThuoc: product.tenKichThuoc || '',
                    giaBan: product.giaBan,
                    soLuong: product.soLuong || 1,
                    soLuongTonKho: product.soLuongTonKho || 99
                });
            }

            this._persist();
            return { success: true, message: 'Đã thêm vào giỏ hàng' };
        },

        removeFromCart(idChiTietSanPham) {
            this.items = this.items.filter((i) => i.idChiTietSanPham !== idChiTietSanPham);
            this._persist();
        },

        updateQuantity(idChiTietSanPham, soLuong) {
            const item = this.items.find((i) => i.idChiTietSanPham === idChiTietSanPham);
            if (item) {
                if (soLuong <= 0) {
                    this.removeFromCart(idChiTietSanPham);
                    return;
                }
                if (soLuong > item.soLuongTonKho) {
                    return { success: false, message: `Chỉ còn ${item.soLuongTonKho} sản phẩm` };
                }
                item.soLuong = soLuong;
                this._persist();
            }
            return { success: true };
        },

        clearCart() {
            this.items = [];
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
