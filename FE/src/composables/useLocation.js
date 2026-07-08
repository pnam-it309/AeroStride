import { ref } from 'vue';
import axios from 'axios';
import api from '@/services/apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export function useLocation(options = {}) {
    const allowFallback = options.allowFallback !== false;
    const provinces = ref([]);
    const districts = ref([]);
    const wards = ref([]);
    const loadingLocations = ref({ provinces: false, districts: false, wards: false });

    // Hàm chuẩn hóa địa danh cực mạnh để khớp lệnh
    const cleanName = (s) => {
        if (!s) return '';
        let str = String(s).toLowerCase().replace(/\s+/g, ' ').trim();

        // Đặc trị các thành phố lớn thường bị viết tắt
        if (str.includes('hồ chí minh') || str.includes('hcm')) return 'hcm';
        if (str.includes('hà nội') || str === 'hn') return 'hanoi';
        if (str.includes('đà nẵng') || str === 'dn') return 'danang';

        // Loại bỏ tiền tố viết tắt và tiền tố đầy đủ
        return str
            .replace(/^(thành phố|tỉnh|quận|huyện|phường|xã|thị xã|thị trấn|tp\.?|t\.?|q\.?|h\.?|x\.?)\s+/gi, '')
            .replace(/\s+/g, '') // Xóa trắng để so sánh chuỗi dính liền
            .trim();
    };

    const matchLocation = (list, name) => {
        if (!name) return null;
        const cleanN = cleanName(name);
        return list.find((x) => cleanName(x.name) === cleanN || cleanName(x.name).includes(cleanN) || cleanN.includes(cleanName(x.name)));
    };

    const extractList = (response) => {
        const body = response?.data ?? response;
        if (Array.isArray(body)) return body;
        if (Array.isArray(body?.data)) return body.data;
        if (Array.isArray(body?.data?.data)) return body.data.data;
        return [];
    };

    const logLocationFallback = (message, error) => {
        if (import.meta.env.DEV) {
            console.warn(message, error?.response?.data?.message || error?.message || error);
        }
    };

    const loadFallbackProvinces = async () => {
        const fallback = await axios.get('https://provinces.open-api.vn/api/p/');
        provinces.value = (fallback.data || []).map((p) => ({
            code: p.code,
            name: p.name,
            source: 'OPEN_API'
        }));
    };

    const loadFallbackDistricts = async (provinceCode) => {
        const fallback = await axios.get(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
        districts.value = (fallback.data?.districts || []).map((d) => ({
            code: d.code,
            name: d.name,
            source: 'OPEN_API'
        }));
    };

    const loadFallbackWards = async (districtCode) => {
        const fallback = await axios.get(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
        wards.value = (fallback.data?.wards || []).map((w) => ({
            code: w.code,
            name: w.name,
            source: 'OPEN_API'
        }));
    };

    const fetchProvinces = async () => {
        if (provinces.value.length) return;
        loadingLocations.value.provinces = true;
        try {
            // Nguồn chính: đi qua backend để giữ token GHN ở server và dùng chung prefix /api/v1.
            const res = await api.get(`${API_ADMIN.GHN}/provinces`, { silent: true });
            const list = extractList(res);
            if (!list.length) throw new Error('GHN provinces empty');
            provinces.value = list.map((p) => ({
                code: p.ProvinceID ?? p.code,
                name: p.ProvinceName ?? p.name,
                source: 'GHN'
            })).filter((p) => p.code && p.name);
        } catch (e) {
            logLocationFallback('GHN provinces unavailable, fallback to open-api.', e);
            if (!allowFallback) return;
            try {
                // Fallback chỉ để form địa chỉ vẫn chọn được khu vực khi GHN tạm lỗi; không dùng mã này để tính phí GHN.
                await loadFallbackProvinces();
            } catch (fallbackError) {
                logLocationFallback('Fallback provinces unavailable.', fallbackError);
            }
        } finally {
            loadingLocations.value.provinces = false;
        }
    };

    const fetchDistricts = async (provinceCode) => {
        if (!provinceCode) return;
        loadingLocations.value.districts = true;
        districts.value = [];
        wards.value = [];
        try {
            // Quận/huyện lấy theo mã GHN từ tỉnh đã chọn; nếu tỉnh là fallback thì dùng open-api.
            const selectedProvince = provinces.value.find((p) => String(p.code) === String(provinceCode));
            if (selectedProvince?.source === 'OPEN_API') {
                if (!allowFallback) return;
                await loadFallbackDistricts(provinceCode);
                return;
            }
            const res = await api.get(`${API_ADMIN.GHN}/districts`, { params: { provinceId: provinceCode }, silent: true });
            const list = extractList(res);
            if (!list.length) throw new Error('GHN districts empty');
            districts.value = list.map((d) => ({
                code: d.DistrictID ?? d.code,
                name: d.DistrictName ?? d.name,
                source: 'GHN'
            })).filter((d) => d.code && d.name);
        } catch (e) {
            logLocationFallback('GHN districts unavailable.', e);
        } finally {
            loadingLocations.value.districts = false;
        }
    };

    const fetchWards = async (districtCode) => {
        if (!districtCode) return;
        loadingLocations.value.wards = true;
        wards.value = [];
        try {
            // Phường/xã đi cùng nguồn với quận/huyện để tránh lệch mã khi tính phí ship.
            const selectedDistrict = districts.value.find((d) => String(d.code) === String(districtCode));
            if (selectedDistrict?.source === 'OPEN_API') {
                if (!allowFallback) return;
                await loadFallbackWards(districtCode);
                return;
            }
            const res = await api.get(`${API_ADMIN.GHN}/wards`, { params: { districtId: districtCode }, silent: true });
            const list = extractList(res);
            if (!list.length) throw new Error('GHN wards empty');
            wards.value = list.map((w) => ({
                code: w.WardCode ?? w.code,
                name: w.WardName ?? w.name,
                source: 'GHN'
            })).filter((w) => w.code && w.name);
        } catch (e) {
            logLocationFallback('GHN wards unavailable.', e);
        } finally {
            loadingLocations.value.wards = false;
        }
    };

    return {
        provinces,
        districts,
        wards,
        loadingLocations,
        fetchProvinces,
        fetchDistricts,
        fetchWards,
        cleanName,
        matchLocation
    };
}
