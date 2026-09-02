import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';
import {
    dichVuThuongHieu,
    dichVuXuatXu,
    dichVuMucDichChay,
    dichVuChatLieu,
    dichVuDeGiay,
    dichVuCoGiay,
    dichVuMauSac,
    dichVuKichThuoc
} from './dichVuThuocTinh';

const toOptionList = (response) => response?.content || response || [];

const loadOptionSafely = async (request) => {
    try {
        return toOptionList(await request({ size: 1000 }));
    } catch (error) {
        return [];
    }
};

export const dichVuSanPham = {
    // Kiem tra ten san pham trung
    async kiemTraTenSanPham(name) {
        const response = await api.get(`${API_ADMIN.SAN_PHAM}/check-name`, { params: { name } });
        return response.data.data;
    },

    _cachedFormOptions: null,
    _inFlightFormOptions: null,

    // Lấy options cho form sản phẩm (có cache & in-flight deduplication)
    async layOptionsForm(forceRefresh = false) {
        if (!forceRefresh && this._cachedFormOptions) {
            return this._cachedFormOptions;
        }
        if (this._inFlightFormOptions) {
            return this._inFlightFormOptions;
        }
        this._inFlightFormOptions = (async () => {
            try {
                const response = await api.get(`${API_ADMIN.SAN_PHAM}/form-options`);
                if (response.data?.data) {
                    this._cachedFormOptions = response.data.data;
                }
                return response.data?.data || this._cachedFormOptions;
            } catch (error) {
                console.error('Error loading form options:', error);
                return {
                    thuongHieus: [],
                    xuatXus: [],
                    mucDichChays: [],
                    chatLieus: [],
                    deGiays: [],
                    coGiays: [],
                    mauSacs: [],
                    kichThuocs: [],
                    trangThais: ['DANG_HOAT_DONG', 'NGUNG_HOAT_DONG'],
                    gioiTinhKhachHangs: ['NAM', 'NU', 'TRE_EM', 'UNISEX']
                };
            } finally {
                this._inFlightFormOptions = null;
            }
        })();
        return this._inFlightFormOptions;
    },

    _cachedProductOptions: null,
    _inFlightProductOptions: null,

    // Lấy danh sách sản phẩm rút gọn cho bộ lọc (có cache & in-flight deduplication)
    async layOptionsSanPham(forceRefresh = false) {
        if (!forceRefresh && this._cachedProductOptions) {
            return this._cachedProductOptions;
        }
        if (this._inFlightProductOptions) {
            return this._inFlightProductOptions;
        }
        this._inFlightProductOptions = (async () => {
            try {
                const response = await api.get(API_ADMIN.SAN_PHAM, { params: { page: 0, size: 1000 } });
                const list = response.data?.data?.content || [];
                if (list.length > 0) {
                    this._cachedProductOptions = list;
                }
                return list;
            } catch (error) {
                console.error('Error loading product options:', error);
                return this._cachedProductOptions || [];
            } finally {
                this._inFlightProductOptions = null;
            }
        })();
        return this._inFlightProductOptions;
    },

    // Lấy danh sách sản phẩm
    async layDanhSachSanPham(params) {
        const response = await api.get(API_ADMIN.SAN_PHAM, { params });
        return response.data.data;
    },

    _detailInFlight: new Map(),

    // Lấy chi tiết sản phẩm (có in-flight deduplication)
    async layChiTietSanPham(id) {
        if (!id) return null;
        if (this._detailInFlight.has(id)) {
            return this._detailInFlight.get(id);
        }
        const promise = (async () => {
            try {
                const response = await api.get(`${API_ADMIN.SAN_PHAM}/${id}`);
                return response.data.data;
            } finally {
                this._detailInFlight.delete(id);
            }
        })();
        this._detailInFlight.set(id, promise);
        return promise;
    },

    // Kiểm tra trùng thuộc tính
    async checkDuplicateAttributes(payload) {
        const response = await api.post(`${API_ADMIN.SAN_PHAM}/check-attributes`, payload);
        return response.data.data;
    },

    // Tạo sản phẩm mới
    async taoSanPham(productData) {
        const response = await api.post(API_ADMIN.SAN_PHAM, productData);
        return response.data.data;
    },

    // Cập nhật sản phẩm
    async capNhatSanPham(id, productData) {
        const response = await api.put(`${API_ADMIN.SAN_PHAM}/${id}`, productData);
        return response.data.data;
    },

    // Xóa sản phẩm
    async xoaSanPham(id) {
        const response = await api.delete(`${API_ADMIN.SAN_PHAM}/${id}`);
        return response.data;
    },

    // Thay đổi trạng thái sản phẩm
    async thayDoiTrangThai(id, status) {
        const response = await api.patch(`${API_ADMIN.SAN_PHAM}/${id}/status`, { status });
        return response.data.data;
    },

    // Xuất Excel
    async xuatExcelSanPham() {
        const response = await api.get(`${API_ADMIN.SAN_PHAM}/export-excel`, { responseType: 'blob' });
        return response.data;
    },

    // Tải template Excel
    async taiTemplateExcel() {
        const response = await api.get(`${API_ADMIN.SAN_PHAM}/download-template`, { responseType: 'blob' });
        return response.data;
    },

    // Nhập Excel
    async nhapExcelSanPham(file) {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post(`${API_ADMIN.SAN_PHAM}/import-excel`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    },

    _cachedMaxPrice: null,

    // Lấy giá lớn nhất (có cache)
    async layGiaLonNhat(forceRefresh = false) {
        if (!forceRefresh && this._cachedMaxPrice !== null) {
            return this._cachedMaxPrice;
        }
        try {
            const response = await api.get(`${API_ADMIN.SAN_PHAM}/max-price`);
            if (response.data?.data) {
                this._cachedMaxPrice = response.data.data;
            }
            return response.data?.data ?? this._cachedMaxPrice;
        } catch (error) {
            return 6500000;
        }
    }
};
