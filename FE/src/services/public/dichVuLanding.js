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
            category: product.tenThuongHieu || product.tenMucDichChay || 'RUNNING',
            summary: product.moTaChiTiet || '',
            imageUrl: resolveImageUrl(product.hinhAnh),
            color: '#2962FF',
            chatLieu: product.tenChatLieu,
            deGiay: product.tenDeGiay,
            coGiay: product.tenCoGiay,
            xuatXu: product.tenXuatXu,
            mucDichChay: product.tenMucDichChay,
            giaBanThapNhat: product.giaBanThapNhat,
            raw: product
        }));
    },
    
    async layDanhSachTinhNang() {
        const response = await api.get('/customer/landing/features');
        return response.data?.data || [];
    }
};
