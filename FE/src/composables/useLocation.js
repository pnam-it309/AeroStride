import { ref, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import api from '@/services/apiService';
import { API_ADMIN } from '@/constants/apiPaths';
import {
    LOCAL_PROVINCES,
    LOCAL_DISTRICTS,
    LOCAL_WARDS,
    getFallbackDistricts,
    getFallbackWards
} from '@/constants/localLocations';

// Singleton in-memory cache to prevent repeated 3-second network timeouts on every component mount
let globalProvincesCache = null;
const globalDistrictsCache = new Map();
const globalWardsCache = new Map();
let inFlightProvincesPromise = null;
const inFlightDistrictsMap = new Map();
const inFlightWardsMap = new Map();

export function useLocation(options = {}) {
    const allowFallback = options.allowFallback !== false;
    const initialProvinces = globalProvincesCache || LOCAL_PROVINCES.map((p) => ({
        code: p.code,
        name: p.name,
        source: 'LOCAL'
    }));
    const provinces = ref(initialProvinces);
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

    const loadLocalProvinces = () => {
        provinces.value = LOCAL_PROVINCES.map((p) => ({
            code: p.code,
            name: p.name,
            source: 'LOCAL'
        }));
    };

    const loadLocalDistricts = (provinceCodeOrName, provinceNameFallback) => {
        let pKey = String(provinceCodeOrName || '');
        let matchedProvince = provinces.value.find((p) => String(p.code) === pKey || String(p.name) === pKey) ||
                              LOCAL_PROVINCES.find((p) => String(p.code) === pKey || cleanName(p.name) === cleanName(pKey));

        let localList = LOCAL_DISTRICTS[pKey] || (matchedProvince ? LOCAL_DISTRICTS[matchedProvince.code] : null);
        const resolvedName = matchedProvince?.name || provinceNameFallback || pKey;

        if (localList && localList.length) {
            districts.value = localList.map((d) => ({
                code: d.code,
                name: d.name,
                source: 'LOCAL'
            }));
        } else {
            districts.value = getFallbackDistricts(resolvedName).map((d) => ({
                code: d.code,
                name: d.name,
                source: 'LOCAL'
            }));
        }
    };

    const loadLocalWards = (districtCodeOrName, districtNameFallback) => {
        let dKey = String(districtCodeOrName || '');
        let localList = LOCAL_WARDS[dKey];

        if (!localList) {
            for (const pKey in LOCAL_DISTRICTS) {
                const found = LOCAL_DISTRICTS[pKey].find((d) => String(d.code) === dKey || cleanName(d.name) === cleanName(dKey) || d.name === dKey);
                if (found) {
                    localList = LOCAL_WARDS[found.code] || LOCAL_WARDS[String(found.code)];
                    dKey = String(found.code);
                    break;
                }
            }
        }

        const resolvedName = districtNameFallback || districtCodeOrName || '';

        if (localList && localList.length) {
            wards.value = localList.map((w) => ({
                code: w.code,
                name: w.name,
                source: 'LOCAL'
            }));
        } else {
            wards.value = getFallbackWards(resolvedName).map((w) => ({
                code: w.code,
                name: w.name,
                source: 'LOCAL'
            }));
        }
    };

    const loadFallbackProvinces = async () => {
        try {
            const fallback = await axios.get('https://provinces.open-api.vn/api/p/', { timeout: 3000 });
            if (fallback.data && fallback.data.length) {
                provinces.value = fallback.data.map((p) => ({
                    code: p.code,
                    name: p.name,
                    source: 'OPEN_API'
                }));
                return;
            }
        } catch (e) {
            logLocationFallback('Open-api provinces failed, using local offline provinces.', e);
        }
        loadLocalProvinces();
    };

    const loadFallbackDistricts = async (provinceCode, provinceName) => {
        try {
            const fallback = await axios.get(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`, { timeout: 3000 });
            if (fallback.data?.districts && fallback.data.districts.length) {
                districts.value = fallback.data.districts.map((d) => ({
                    code: d.code,
                    name: d.name,
                    source: 'OPEN_API'
                }));
                return;
            }
        } catch (e) {
            logLocationFallback('Open-api districts failed, using local offline districts.', e);
        }
        loadLocalDistricts(provinceCode, provinceName);
    };

    const loadFallbackWards = async (districtCode, districtName) => {
        try {
            const fallback = await axios.get(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`, { timeout: 3000 });
            if (fallback.data?.wards && fallback.data.wards.length) {
                wards.value = fallback.data.wards.map((w) => ({
                    code: w.code,
                    name: w.name,
                    source: 'OPEN_API'
                }));
                return;
            }
        } catch (e) {
            logLocationFallback('Open-api wards failed, using local offline wards.', e);
        }
        loadLocalWards(districtCode, districtName);
    };

    const fetchProvinces = async (forceRefresh = false) => {
        if (globalProvincesCache && globalProvincesCache.length && !forceRefresh) {
            provinces.value = globalProvincesCache;
            return globalProvincesCache;
        }
        if (inFlightProvincesPromise) {
            const list = await inFlightProvincesPromise;
            provinces.value = list;
            return list;
        }

        loadingLocations.value.provinces = true;
        inFlightProvincesPromise = (async () => {
            try {
                const res = await api.get(`${API_ADMIN.GHN}/provinces`, { silent: true, timeout: 3000 });
                const list = extractList(res);
                if (!list.length) throw new Error('GHN provinces empty');
                const result = list
                    .map((p) => ({
                        code: p.ProvinceID ?? p.code,
                        name: p.ProvinceName ?? p.name,
                        source: 'GHN'
                    }))
                    .filter((p) => p.code && p.name);
                globalProvincesCache = result;
                return result;
            } catch (e) {
                logLocationFallback('GHN provinces unavailable, fallback to open-api / local.', e);
                if (!allowFallback) {
                    loadLocalProvinces();
                    globalProvincesCache = provinces.value;
                    return provinces.value;
                }
                try {
                    await loadFallbackProvinces();
                } catch (fallbackError) {
                    loadLocalProvinces();
                }
                globalProvincesCache = provinces.value;
                return provinces.value;
            } finally {
                if (!provinces.value.length && (!globalProvincesCache || !globalProvincesCache.length)) {
                    loadLocalProvinces();
                    globalProvincesCache = provinces.value;
                }
                loadingLocations.value.provinces = false;
                inFlightProvincesPromise = null;
            }
        })();

        const list = await inFlightProvincesPromise;
        provinces.value = list;
        return list;
    };

    const fetchDistricts = async (provinceCode) => {
        if (!provinceCode) return [];
        const pKey = String(provinceCode);
        if (globalDistrictsCache.has(pKey)) {
            districts.value = globalDistrictsCache.get(pKey);
            return districts.value;
        }
        if (inFlightDistrictsMap.has(pKey)) {
            const list = await inFlightDistrictsMap.get(pKey);
            districts.value = list;
            return list;
        }

        loadingLocations.value.districts = true;
        districts.value = [];
        wards.value = [];
        const selectedProvince = provinces.value.find((p) => String(p.code) === pKey);

        const fetchPromise = (async () => {
            try {
                const res = await api.get(`${API_ADMIN.GHN}/districts`, { params: { provinceId: provinceCode }, silent: true, timeout: 3000 });
                const list = extractList(res);
                if (!list.length) throw new Error('GHN districts empty');
                const result = list
                    .map((d) => ({
                        code: d.DistrictID ?? d.code,
                        name: d.DistrictName ?? d.name,
                        source: 'GHN'
                    }))
                    .filter((d) => d.code && d.name);
                globalDistrictsCache.set(pKey, result);
                return result;
            } catch (e) {
                logLocationFallback('GHN districts unavailable, fallback to open-api / local.', e);
                if (selectedProvince?.source === 'LOCAL') {
                    loadLocalDistricts(provinceCode, selectedProvince?.name);
                } else {
                    await loadFallbackDistricts(provinceCode, selectedProvince?.name);
                }
                globalDistrictsCache.set(pKey, districts.value);
                return districts.value;
            } finally {
                if (!districts.value.length) {
                    loadLocalDistricts(provinceCode, selectedProvince?.name);
                    globalDistrictsCache.set(pKey, districts.value);
                }
                loadingLocations.value.districts = false;
                inFlightDistrictsMap.delete(pKey);
            }
        })();

        inFlightDistrictsMap.set(pKey, fetchPromise);
        const list = await fetchPromise;
        districts.value = list;
        return list;
    };

    const fetchWards = async (districtCode) => {
        if (!districtCode) return [];
        const dKey = String(districtCode);
        if (globalWardsCache.has(dKey)) {
            wards.value = globalWardsCache.get(dKey);
            return wards.value;
        }
        if (inFlightWardsMap.has(dKey)) {
            const list = await inFlightWardsMap.get(dKey);
            wards.value = list;
            return list;
        }

        loadingLocations.value.wards = true;
        wards.value = [];
        const selectedDistrict = districts.value.find((d) => String(d.code) === dKey);

        const fetchPromise = (async () => {
            try {
                const res = await api.get(`${API_ADMIN.GHN}/wards`, { params: { districtId: districtCode }, silent: true, timeout: 3000 });
                const list = extractList(res);
                if (!list.length) throw new Error('GHN wards empty');
                const result = list
                    .map((w) => ({
                        code: w.WardCode ?? w.code,
                        name: w.WardName ?? w.name,
                        source: 'GHN'
                    }))
                    .filter((w) => w.code && w.name);
                globalWardsCache.set(dKey, result);
                return result;
            } catch (e) {
                logLocationFallback('GHN wards unavailable, fallback to open-api / local.', e);
                if (selectedDistrict?.source === 'LOCAL') {
                    loadLocalWards(districtCode, selectedDistrict?.name);
                } else {
                    await loadFallbackWards(districtCode, selectedDistrict?.name);
                }
                globalWardsCache.set(dKey, wards.value);
                return wards.value;
            } finally {
                if (!wards.value.length) {
                    loadLocalWards(districtCode, selectedDistrict?.name);
                    globalWardsCache.set(dKey, wards.value);
                }
                loadingLocations.value.wards = false;
                inFlightWardsMap.delete(dKey);
            }
        })();

        inFlightWardsMap.set(dKey, fetchPromise);
        const list = await fetchPromise;
        wards.value = list;
        return list;
    };

    // Tu dong lang nghe su kien bat mang tro lai (Online) de chuyen ve Live API
    const handleOnline = () => {
        if (provinces.value.some((p) => p.source === 'LOCAL' || p.source === 'OPEN_API')) {
            fetchProvinces(true);
        }
    };

    if (typeof window !== 'undefined') {
        window.addEventListener('online', handleOnline);
    }

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
