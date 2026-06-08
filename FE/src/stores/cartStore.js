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

        // Thêm một sản phẩm (kèm số lượng, kích thước, màu sắc) vào giỏ hàng
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

        // Xóa hoàn toàn một sản phẩm khỏi giỏ hàng dựa trên ID chi tiết sản phẩm
        removeFromCart(idChiTietSanPham) {
            this.items = this.items.filter((i) => i.idChiTietSanPham !== idChiTietSanPham);
            this._persist();
        },

        // Cập nhật số lượng của một sản phẩm đã có trong giỏ hàng
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

        // Xóa toàn bộ sản phẩm trong giỏ hàng (làm trống giỏ hàng)
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
