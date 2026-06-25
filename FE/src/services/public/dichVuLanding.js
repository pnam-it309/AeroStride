import api from '../apiService';
import { dichVuFile } from '@/services/core/dichVuFile';

const isAbsoluteUrl = (value) => /^(https?:)?\/\//i.test(value) || value?.startsWith('data:') || value?.startsWith('blob:');

const resolveImageUrl = (value) => {
    if (!value) return '';
    if (isAbsoluteUrl(value)) return value;
    return dichVuFile.layUrlFile(value.replace(/^\/+/, ''));
};

export const dichVuLanding = {
    async laySanPhamNoiBat(size = 6) {
        const response = await api.get('/customer/landing/products', {
            params: { size }
        });

        return (response.data?.data || []).map((product, index) => ({
            id: product.id,
            index,
            title: product.tenSanPham,
            subtitle: product.tenThuongHieu || '',
            summary: product.moTaNgan || '',
            imageUrl: resolveImageUrl(product.hinhAnh),
            color: '#2962FF',
            raw: product
        }));
    },

    async layBienTheNoiBat(size = 12) {
        const response = await api.get('/customer/landing/featured-variants', {
            params: { size }
        });

        return (response.data?.data || []).map((variant) => ({
            id: variant.id,
            idSanPham: variant.idSanPham,
            maSanPham: variant.maSanPham,
            tenSanPham: variant.tenSanPham,
            tenThuongHieu: variant.tenThuongHieu,
            maChiTietSanPham: variant.maChiTietSanPham,
            tenMauSac: variant.tenMauSac,
            maMauHex: variant.maMauHex,
            tenKichThuoc: variant.tenKichThuoc,
            giaTriKichThuoc: variant.giaTriKichThuoc,
            soLuong: variant.soLuong,
            giaBan: variant.giaBan,
            phanTramGiam: variant.phanTramGiam,
            hinhAnh: resolveImageUrl(variant.hinhAnh),
            images: (variant.images || []).map(resolveImageUrl)
        }));
    },

    async layTopBienTheTheoSoLuong(size = 5) {
        const response = await api.get('/customer/landing/top-variants', {
            params: { size }
        });

        return (response.data?.data || []).map((variant) => ({
            id: variant.id,
            idSanPham: variant.idSanPham,
            maSanPham: variant.maSanPham,
            tenSanPham: variant.tenSanPham,
            tenThuongHieu: variant.tenThuongHieu,
            maChiTietSanPham: variant.maChiTietSanPham,
            tenMauSac: variant.tenMauSac,
            maMauHex: variant.maMauHex,
            tenKichThuoc: variant.tenKichThuoc,
            soLuong: variant.soLuong,
            giaBan: variant.giaBan,
            phanTramGiam: variant.phanTramGiam,
            hinhAnh: resolveImageUrl(variant.hinhAnh),
            images: (variant.images || []).map(resolveImageUrl)
        }));
    },

    async layDanhSachTinhNang() {
        const response = await api.get('/customer/landing/features');
        return response.data?.data || [];
    }
};
