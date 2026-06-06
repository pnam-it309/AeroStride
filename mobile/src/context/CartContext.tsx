/**
 * Cart Context for AeroStride Mobile
 * Pattern matches FE/src/stores/cartStore.js
 */

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { storage } from '@/utils/storage';

export interface CartItem {
  idChiTietSanPham: string;
  tenSanPham: string;
  hinhAnh: string;
  tenMauSac: string;
  tenKichThuoc: string;
  giaBan: number;
  soLuong: number;
  soLuongTonKho: number;
}

interface CartContextType {
  items: CartItem[];
  cartCount: number;
  cartTotal: number;
  isEmpty: boolean;
  addToCart: (product: CartItem) => { success: boolean; message: string };
  removeFromCart: (idChiTietSanPham: string) => void;
  updateQuantity: (idChiTietSanPham: string, soLuong: number) => { success: boolean; message?: string };
  clearCart: () => void;
  isLoading: boolean;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Load cart from AsyncStorage on mount
  useEffect(() => {
    (async () => {
      try {
        const savedCart = await storage.getCart();
        setItems(savedCart);
      } catch {
        setItems([]);
      } finally {
        setIsLoading(false);
      }
    })();
  }, []);

  // Persist cart whenever items change
  useEffect(() => {
    if (!isLoading) {
      storage.setCart(items);
    }
  }, [items, isLoading]);

  const cartCount = items.reduce((sum, item) => sum + item.soLuong, 0);
  const cartTotal = items.reduce((sum, item) => sum + item.giaBan * item.soLuong, 0);
  const isEmpty = items.length === 0;

  const addToCart = useCallback(
    (product: CartItem): { success: boolean; message: string } => {
      const existing = items.find((i) => i.idChiTietSanPham === product.idChiTietSanPham);

      if (existing) {
        const newQty = existing.soLuong + (product.soLuong || 1);
        if (newQty > (product.soLuongTonKho || 99)) {
          return {
            success: false,
            message: `Chỉ còn ${product.soLuongTonKho} sản phẩm trong kho`,
          };
        }
        setItems((prev) =>
          prev.map((i) =>
            i.idChiTietSanPham === product.idChiTietSanPham
              ? { ...i, soLuong: newQty }
              : i
          )
        );
      } else {
        setItems((prev) => [
          ...prev,
          {
            idChiTietSanPham: product.idChiTietSanPham,
            tenSanPham: product.tenSanPham,
            hinhAnh: product.hinhAnh || '',
            tenMauSac: product.tenMauSac || '',
            tenKichThuoc: product.tenKichThuoc || '',
            giaBan: product.giaBan,
            soLuong: product.soLuong || 1,
            soLuongTonKho: product.soLuongTonKho || 99,
          },
        ]);
      }

      return { success: true, message: 'Đã thêm vào giỏ hàng' };
    },
    [items]
  );

  const removeFromCart = useCallback((idChiTietSanPham: string) => {
    setItems((prev) => prev.filter((i) => i.idChiTietSanPham !== idChiTietSanPham));
  }, []);

  const updateQuantity = useCallback(
    (idChiTietSanPham: string, soLuong: number): { success: boolean; message?: string } => {
      if (soLuong <= 0) {
        removeFromCart(idChiTietSanPham);
        return { success: true };
      }

      const item = items.find((i) => i.idChiTietSanPham === idChiTietSanPham);
      if (item && soLuong > item.soLuongTonKho) {
        return { success: false, message: `Chỉ còn ${item.soLuongTonKho} sản phẩm` };
      }

      setItems((prev) =>
        prev.map((i) =>
          i.idChiTietSanPham === idChiTietSanPham ? { ...i, soLuong } : i
        )
      );
      return { success: true };
    },
    [items, removeFromCart]
  );

  const clearCart = useCallback(() => {
    setItems([]);
  }, []);

  return (
    <CartContext.Provider
      value={{
        items,
        cartCount,
        cartTotal,
        isEmpty,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
        isLoading,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart(): CartContextType {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}
