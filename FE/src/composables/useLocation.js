import { ref } from 'vue';
import axios from 'axios';

export function useLocation() {
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

    const fetchProvinces = async () => {
        if (provinces.value.length) return;
        loadingLocations.value.provinces = true;
        try {
            const res = await axios.get(`${import.meta.env.VITE_API_URL}/api/admin/ghn/provinces`);
            if (res.data && res.data.data) {
                provinces.value = res.data.data.map(p => ({
                    code: p.ProvinceID,
                    name: p.ProvinceName
                }));
            }
        } catch (e) {
            console.error('Error loading provinces:', e);
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
            const res = await axios.get(`${import.meta.env.VITE_API_URL}/api/admin/ghn/districts?provinceId=${provinceCode}`);
            if (res.data && res.data.data) {
                districts.value = res.data.data.map(d => ({
                    code: d.DistrictID,
                    name: d.DistrictName
                }));
            }
        } catch (e) {
            console.error('Error loading districts:', e);
        } finally {
            loadingLocations.value.districts = false;
        }
    };

    const fetchWards = async (districtCode) => {
        if (!districtCode) return;
        loadingLocations.value.wards = true;
        wards.value = [];
        try {
            const res = await axios.get(`${import.meta.env.VITE_API_URL}/api/admin/ghn/wards?districtId=${districtCode}`);
            if (res.data && res.data.data) {
                wards.value = res.data.data.map(w => ({
                    code: w.WardCode,
                    name: w.WardName
                }));
            }
        } catch (e) {
            console.error('Error loading wards:', e);
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
