<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { BoxIcon, XIcon } from 'vue-tabler-icons';
import { Html5Qrcode } from 'html5-qrcode';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuThuongHieu, dichVuMucDichChay, dichVuMauSac, dichVuKichThuoc } from '@/services/product/dichVuThuocTinh';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import { formatCurrency } from '@/utils/formatters';
import { useBanHangStore } from '@/stores/banHangStore';
import SafeProductImage from '@/views/modules/san-pham/components/SafeProductImage.vue';

const emit = defineEmits(['add-product']);
const props = defineProps({
    activeOrder: {
        type: Object,
        default: null
    }
});
const store = useBanHangStore();
const { addNotification } = useNotifications();

let lastSearchParamsKey = '';

// Data loading
const fetchProductSearchResults = async (keyword, force = false) => {
    let minGia = undefined;
    let maxGia = undefined;
    if (store.filterKhoangGia && store.filterKhoangGia !== 'ALL') {
        const option = priceRangeOptions.value.find((o) => o.value === store.filterKhoangGia);
        if (option) {
            minGia = option.min;
            maxGia = option.max;
        }
    }
    const paramsKey = JSON.stringify({
        kw: (keyword || '').trim(),
        th: store.filterThuongHieu,
        md: store.filterMucDich,
        ms: store.filterMauSac,
        kc: store.filterKichCo,
        min: minGia,
        max: maxGia
    });

    if (!force && paramsKey === lastSearchParamsKey && store.productSearchResults?.length > 0) {
        return;
    }
    lastSearchParamsKey = paramsKey;

    store.productSearchLoading = true;
    try {
        const params = {
            keyword: (keyword || '').trim(),
            thuongHieu: store.filterThuongHieu,
            mucDich: store.filterMucDich,
            mauSac: store.filterMauSac,
            kichCo: store.filterKichCo,
            minGia: minGia,
            maxGia: maxGia
        };
        const res = await dichVuDonHang.searchSanPham(params);
        const list = Array.isArray(res) ? res : [];
        store.productSearchResults = list.filter((item) => Number(item.soLuongTon ?? item.soLuong ?? 0) > 0);
    } catch (e) {
        console.error('Lỗi khi tải sản phẩm:', e);
    } finally {
        store.productSearchLoading = false;
    }
};

const loadFilterOptions = async (force = false) => {
    if (!force && store.filterBrands?.length > 1) {
        return;
    }
    try {
        const [th, md, ms, kt, maxPriceRes] = await Promise.allSettled([
            dichVuThuongHieu.layThuongHieu({ size: 1000 }),
            dichVuMucDichChay.layMucDichChay({ size: 1000 }),
            dichVuMauSac.layMauSac({ size: 1000 }),
            dichVuKichThuoc.layKichThuoc({ size: 1000 }),
            dichVuSanPham.layGiaLonNhat()
        ]);

        const pick = (res) => {
            if (res.status === 'fulfilled') {
                const val = res.value;
                if (Array.isArray(val)) return val;
                if (val && Array.isArray(val.content)) return val.content;
                if (val && Array.isArray(val.data)) return val.data;
            }
            return [];
        };

        store.filterBrands = [{ title: 'Thương hiệu', value: 'ALL' }, ...pick(th).map((x) => ({ title: x.ten, value: x.ten }))];
        store.filterPurposes = [{ title: 'Mục đích', value: 'ALL' }, ...pick(md).map((x) => ({ title: x.ten, value: x.ten }))];
        store.filterColors = [{ title: 'Màu sắc', value: 'ALL' }, ...pick(ms).map((x) => ({ title: x.ten, value: x.ten }))];
        store.filterSizes = [{ title: 'Kích cỡ', value: 'ALL' }, ...pick(kt).map((x) => ({ title: x.ten, value: x.ten }))];

        if (maxPriceRes.status === 'fulfilled' && maxPriceRes.value) {
            store.maxProductPrice = Number(maxPriceRes.value);
        }
    } catch (e) {
        console.error('Lỗi khi tải bộ lọc:', e);
    }
};

const priceRangeOptions = computed(() => {
    const list = [
        { title: 'Khoảng giá', value: 'ALL' },
        { title: '0 - 500.000đ', value: '0-500000', min: 0, max: 500000 }
    ];

    let currentMin = 500000;
    let currentMax = 1000000;
    const maxVal = store.maxProductPrice;

    while (currentMin < maxVal) {
        const minStr = new Intl.NumberFormat('vi-VN').format(currentMin);
        const maxStr = new Intl.NumberFormat('vi-VN').format(currentMax);
        list.push({
            title: `${minStr}đ - ${maxStr}đ`,
            value: `${currentMin}-${currentMax}`,
            min: currentMin,
            max: currentMax
        });
        currentMin = currentMax;
        currentMax = currentMin + 1000000;
    }
    return list;
});

const filteredProductSearchResults = computed(() => {
    const list = store.productSearchResults || [];
    return list.filter((item) => Number(item.soLuongTon ?? item.soLuong ?? 0) > 0);
});

const onFilterChange = () => {
    store.showProductAutocomplete = true;
};

// Lưu trữ filter state độc lập cho từng hóa đơn
const orderFiltersMap = ref({});

const getDefaultFilter = () => ({
    keyword: '',
    thuongHieu: 'ALL',
    mucDich: 'ALL',
    khoangGia: 'ALL',
    mauSac: 'ALL',
    kichCo: 'ALL'
});

let isRestoringOrderFilter = false;

// Watch khi chuyển tab hóa đơn hoặc tạo hóa đơn mới
watch(
    () => props.activeOrder?.id,
    (newOrderId, oldOrderId) => {
        if (oldOrderId) {
            orderFiltersMap.value[oldOrderId] = {
                keyword: store.productSearchKeyword,
                thuongHieu: store.filterThuongHieu,
                mucDich: store.filterMucDich,
                khoangGia: store.filterKhoangGia,
                mauSac: store.filterMauSac,
                kichCo: store.filterKichCo
            };
        }

        isRestoringOrderFilter = true;
        const current = (newOrderId && orderFiltersMap.value[newOrderId]) ? orderFiltersMap.value[newOrderId] : getDefaultFilter();

        store.filterThuongHieu = current.thuongHieu || 'ALL';
        store.filterMucDich = current.mucDich || 'ALL';
        store.filterKhoangGia = current.khoangGia || 'ALL';
        store.filterMauSac = current.mauSac || 'ALL';
        store.filterKichCo = current.kichCo || 'ALL';
        store.productSearchKeyword = current.keyword || '';
        store.showProductAutocomplete = false;
        lastSearchParamsKey = '';

        setTimeout(() => {
            isRestoringOrderFilter = false;
            const hasActive =
                (store.productSearchKeyword && store.productSearchKeyword.trim() !== '') ||
                store.filterThuongHieu !== 'ALL' ||
                store.filterMucDich !== 'ALL' ||
                store.filterKhoangGia !== 'ALL' ||
                store.filterMauSac !== 'ALL' ||
                store.filterKichCo !== 'ALL';

            if (hasActive) {
                fetchProductSearchResults(store.productSearchKeyword, true);
            } else {
                store.productSearchResults = [];
            }
        }, 50);
    },
    { immediate: true }
);

const resetFilters = () => {
    store.resetFilters();
    if (props.activeOrder?.id) {
        orderFiltersMap.value[props.activeOrder.id] = getDefaultFilter();
    }
    lastSearchParamsKey = '';
    store.productSearchResults = [];
};

let searchDebounce = null;
watch(
    [
        () => store.productSearchKeyword,
        () => store.filterThuongHieu,
        () => store.filterMucDich,
        () => store.filterKhoangGia,
        () => store.filterMauSac,
        () => store.filterKichCo
    ],
    () => {
        if (isRestoringOrderFilter) return;

        if (props.activeOrder?.id) {
            orderFiltersMap.value[props.activeOrder.id] = {
                keyword: store.productSearchKeyword,
                thuongHieu: store.filterThuongHieu,
                mucDich: store.filterMucDich,
                khoangGia: store.filterKhoangGia,
                mauSac: store.filterMauSac,
                kichCo: store.filterKichCo
            };
        }

        if (searchDebounce) clearTimeout(searchDebounce);
        searchDebounce = setTimeout(() => {
            fetchProductSearchResults(store.productSearchKeyword, true);
        }, 300);
    }
);

const onProductSearchFocus = () => {
    store.showProductAutocomplete = true;
    fetchProductSearchResults(store.productSearchKeyword, false);
};

const onProductSearchBlur = () => {
    setTimeout(() => {
        store.showProductAutocomplete = false;
    }, 250);
};

const selectProductFromSearch = (variant) => {
    emit('add-product', { ...variant, _soLuongMuonThem: 1 });
    store.productSearchKeyword = '';
    store.showProductAutocomplete = false;
    if (props.activeOrder?.id && orderFiltersMap.value[props.activeOrder.id]) {
        orderFiltersMap.value[props.activeOrder.id].keyword = '';
    }
};

// Scanner Logic
const showScanner = ref(false);
let html5QrcodeScanner = null;
const scannerElementId = 'product-picker-qr-reader';

const startScanner = () => {
    showScanner.value = true;
    setTimeout(async () => {
        const el = document.getElementById(scannerElementId);
        if (!el || el.clientWidth === 0) {
            if (showScanner.value) startScanner();
            return;
        }

        if (html5QrcodeScanner) {
            await stopScanner(false);
        }

        try {
            // Mo camera truc tiep de popup hien hinh camera ngay, khong dung man request mac dinh cua thu vien.
            html5QrcodeScanner = new Html5Qrcode(scannerElementId);
            await html5QrcodeScanner.start(
                { facingMode: 'environment' },
                { fps: 10, qrbox: { width: 250, height: 250 } },
                onScanSuccess,
                onScanFailure
            );
        } catch (error) {
            console.error('Camera start error:', error);
            addNotification({
                title: 'Không mở được camera',
                subtitle: 'Vui lòng cấp quyền camera cho trình duyệt hoặc kiểm tra camera đang bị ứng dụng khác dùng.',
                color: 'error'
            });
            await stopScanner();
        }
    }, 150);
};

const stopScanner = async (closeDialog = true) => {
    if (html5QrcodeScanner) {
        try {
            await html5QrcodeScanner.stop();
            await html5QrcodeScanner.clear();
        } catch (error) {
            console.error('Failed to stop scanner', error);
        }
        html5QrcodeScanner = null;
    }
    if (closeDialog) showScanner.value = false;
};

import ScannerDialog from './ScannerDialog.vue';
import { isActiveStatus } from '@/utils/statusUtils';

const onScanSuccess = async (decodedText) => {
    await stopScanner();
    let keyword = decodedText?.trim();
    if (!keyword) return;

    try {
        if (keyword.startsWith('{') && keyword.endsWith('}')) {
            const parsed = JSON.parse(keyword);
            keyword = parsed.maChiTietSanPham || parsed.ma || parsed.id || keyword;
        }
    } catch (e) {
        // regular text
    }

    // Hóa đơn đang chọn được quản lý ở BanHang.vue; dùng prop để quét QR không bị lệch state Pinia.
    if (!props.activeOrder?.id) {
        addNotification({ title: 'Chưa có hóa đơn', subtitle: 'Vui lòng tạo hoặc chọn hóa đơn trước khi quét mã.', color: 'warning' });
        return;
    }

    try {
        const variants = await dichVuDonHang.searchSanPham({ keyword });
        if (variants && variants.length > 0) {
            const normalizedKeyword = keyword.toLowerCase();
            const exactMatch =
                variants.find(
                    (v) =>
                        String(v.maChiTietSanPham || '')
                            .trim()
                            .toLowerCase() === normalizedKeyword ||
                        String(v.id || '')
                            .trim()
                            .toLowerCase() === normalizedKeyword ||
                        String(v.maSanPham || '')
                            .trim()
                            .toLowerCase() === normalizedKeyword
                ) || variants[0];

            if (exactMatch) {
                if (exactMatch.trangThai !== undefined && !isActiveStatus(exactMatch.trangThai)) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã ngừng bán', color: 'error' });
                    return;
                }
                const currentStock = Number(exactMatch.soLuongTon ?? exactMatch.soLuong ?? 0);
                if (currentStock <= 0) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã hết hàng trong kho', color: 'error' });
                    return;
                }
                emit('add-product', { ...exactMatch, _soLuongMuonThem: 1 });
            }
        } else {
            addNotification({ title: 'Không tìm thấy', subtitle: `Không tìm thấy mã sản phẩm [${keyword}]`, color: 'warning' });
        }
    } catch (e) {
        console.error('Scan error:', e);
    }
};

const onScanFile = async (file) => {
    if (!file) return;
    try {
        if (!html5QrcodeScanner) {
            html5QrcodeScanner = new Html5Qrcode(scannerElementId);
        }
        if (html5QrcodeScanner.isScanning) {
            try {
                await html5QrcodeScanner.stop();
            } catch (e) {}
        }
        const decodedText = await html5QrcodeScanner.scanFile(file, true);
        if (decodedText) {
            onScanSuccess(decodedText);
        }
    } catch (err) {
        console.error('Lỗi quét file ảnh QR:', err);
        addNotification({
            title: 'Không nhận diện được QR',
            subtitle: 'Không thể tìm thấy mã QR trong file ảnh đã chọn. Vui lòng thử ảnh rõ nét hơn.',
            color: 'warning'
        });
    }
};

const onScanFailure = (error) => {
    // Console error ignored
};

onMounted(() => {
    loadFilterOptions();
});

onUnmounted(() => {
    stopScanner();
});
</script>

<template>
    <div class="product-picker-panel d-flex flex-column ga-2 mb-3 bg-slate-50 pa-2 rounded-xl w-100">
        <!-- Hàng 1: Các Combobox lọc (CBO) lên trên -->
        <div class="d-flex align-center ga-2 flex-wrap">
            <!-- Thương hiệu -->
            <div style="min-width: 130px; flex: 1 1 130px">
                <v-select
                    v-model="store.filterThuongHieu"
                    :items="store.filterBrands"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    bg-color="white"
                    class="compact-select"
                    :menu-props="{ contentClass: 'pos-select-menu' }"
                    @update:model-value="onFilterChange"
                />
            </div>

            <!-- Mục đích chạy -->
            <div style="min-width: 130px; flex: 1 1 130px">
                <v-select
                    v-model="store.filterMucDich"
                    :items="store.filterPurposes"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    bg-color="white"
                    class="compact-select"
                    :menu-props="{ contentClass: 'pos-select-menu' }"
                    @update:model-value="onFilterChange"
                />
            </div>

            <!-- Khoảng giá -->
            <div style="min-width: 135px; flex: 1 1 135px">
                <v-select
                    v-model="store.filterKhoangGia"
                    :items="priceRangeOptions"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    bg-color="white"
                    class="compact-select"
                    :menu-props="{ contentClass: 'pos-select-menu' }"
                    @update:model-value="onFilterChange"
                />
            </div>

            <!-- Màu sắc -->
            <div style="min-width: 110px; flex: 1 1 110px">
                <v-select
                    v-model="store.filterMauSac"
                    :items="store.filterColors"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    bg-color="white"
                    class="compact-select"
                    :menu-props="{ contentClass: 'pos-select-menu' }"
                    @update:model-value="onFilterChange"
                />
            </div>

            <!-- Kích cỡ -->
            <div style="min-width: 110px; flex: 1 1 110px">
                <v-select
                    v-model="store.filterKichCo"
                    :items="store.filterSizes"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    bg-color="white"
                    class="compact-select"
                    :menu-props="{ contentClass: 'pos-select-menu' }"
                    @update:model-value="onFilterChange"
                />
            </div>
        </div>

        <!-- Hàng 2: Input tìm kiếm và các nút -->
        <div class="d-flex align-center ga-2">
            <!-- Custom Search Input with floating Dropdown -->
            <div class="position-relative flex-grow-1">
                <v-text-field
                    v-model="store.productSearchKeyword"
                    placeholder="Nhập mã, tên sản phẩm"
                    variant="outlined"
                    density="compact"
                    hide-details
                    prepend-inner-icon="mdi-magnify"
                    @focus="onProductSearchFocus"
                    @click="onProductSearchFocus"
                    @blur="onProductSearchBlur"
                    bg-color="white"
                    class="search-input"
                    autocomplete="off"
                />

                <!-- Search dropdown overlay -->
                <v-card
                    v-if="store.showProductAutocomplete && filteredProductSearchResults.length > 0"
                    class="position-absolute mt-1 elevation-4 border rounded-lg overflow-y-auto product-dropdown-card"
                    style="max-height: 495px !important; z-index: 1000; width: 100%"
                >
                    <v-list class="pa-2" style="background-color: #f8fafc">
                        <v-list-item
                            v-for="variant in filteredProductSearchResults"
                            :key="variant.id"
                            @mousedown="selectProductFromSearch(variant)"
                            class="product-search-item pointer"
                        >
                            <div class="d-flex justify-space-between w-100 align-start">
                                <!-- Left info block -->
                                <div class="d-flex align-start flex-grow-1">
                                    <v-avatar rounded="lg" size="48" class="mr-3 bg-grey-lighten-4 border flex-shrink-0 overflow-hidden">
                                        <SafeProductImage :src="variant.hinhAnh" :alt="variant.tenSanPham" :iconSize="24" />
                                    </v-avatar>
                                    <div class="d-flex flex-column" style="gap: 8px !important">
                                        <div class="text-slate-700" style="font-size: 13.5px !important; line-height: 1.3">
                                            {{ variant.tenSanPham }}
                                        </div>
                                        <div class="d-flex align-center ga-2 mt-1 flex-wrap">
                                            <span class="sp-badge">Mã Sản phẩm: {{ variant.maSanPham || 'SP0001' }}</span>
                                            <span
                                                style="margin-left: 15px; margin-right: 15px; font-size: 11px; color: #cbd5e1; opacity: 0.4"
                                                >|</span
                                            >
                                            <span class="sku-badge">{{ variant.maChiTietSanPham }}</span>
                                        </div>
                                        <div class="d-flex align-center mt-1 text-slate-600" style="font-size: 12px; flex-wrap: wrap">
                                            <span
                                                >Màu sắc: <span class="text-slate-500">{{ variant.tenMauSac || 'Không màu' }}</span></span
                                            >
                                            <span style="margin-left: 15px; margin-right: 15px; color: #cbd5e1; opacity: 0.4">|</span>
                                            <span
                                                >Size: <span class="text-slate-500">{{ variant.tenKichThuoc || 'N/A' }}</span></span
                                            >
                                            <span style="margin-left: 15px; margin-right: 15px; color: #cbd5e1; opacity: 0.4">|</span>
                                            <span
                                                >Số lượng: <span class="text-slate-500">{{ variant.soLuongTon || 0 }}</span></span
                                            >
                                        </div>
                                    </div>
                                </div>

                                <!-- Right info block -->
                                <div class="text-right flex-shrink-0 pl-3 d-flex flex-column align-end justify-center">
                                    <template v-if="variant.phanTramGiam > 0">
                                        <div class="d-flex align-center ga-1">
                                            <span class="price-text font-weight-bold" style="color: #dc2626 !important">{{ formatCurrency(variant.giaBan) }}</span>
                                            <v-chip size="x-small" color="error" variant="flat" class="font-weight-bold px-1" style="height: 16px; font-size: 10px">-{{ variant.phanTramGiam }}%</v-chip>
                                        </div>
                                        <span
                                            class="text-caption text-slate-400 text-decoration-line-through"
                                            style="font-size: 11px !important; margin-top: 1px"
                                        >
                                            {{ formatCurrency(variant.giaGoc || variant.giaBan / (1 - variant.phanTramGiam / 100)) }}
                                        </span>
                                    </template>
                                    <template v-else>
                                        <div class="price-text">{{ formatCurrency(variant.giaBan) }}</div>
                                    </template>
                                </div>
                            </div>
                        </v-list-item>
                    </v-list>
                </v-card>
                <v-card
                    v-else-if="store.showProductAutocomplete && !store.productSearchLoading"
                    class="position-absolute w-100 mt-1 elevation-4 border rounded-lg pa-4 text-center text-grey"
                    style="z-index: 1000"
                >
                    Không tìm thấy sản phẩm phù hợp.
                </v-card>
            </div>

            <!-- Scanner Button -->
            <v-tooltip text="Quét mã vạch" location="top" open-delay="0" content-class="custom-white-tooltip">
                <template v-slot:activator="{ props }">
                    <v-btn
                        color="primary"
                        variant="outlined"
                        class="scanner-btn text-none px-1"
                        style="width: 44px; min-width: 44px; flex-shrink: 0"
                        @click="startScanner"
                        v-bind="props"
                    >
                        <v-icon>mdi-barcode-scan</v-icon>
                    </v-btn>
                </template>
            </v-tooltip>

            <!-- Reset Filters Button -->
            <v-tooltip text="Làm mới bộ lọc" location="top" open-delay="0" content-class="custom-white-tooltip">
                <template v-slot:activator="{ props }">
                    <v-btn
                        color="error"
                        variant="outlined"
                        class="scanner-btn text-none px-1"
                        style="width: 44px; min-width: 44px; flex-shrink: 0"
                        @click="resetFilters"
                        v-bind="props"
                    >
                        <v-icon>mdi-filter-off</v-icon>
                    </v-btn>
                </template>
            </v-tooltip>
        </div>

        <!-- QR Scanner Dialog -->
        <ScannerDialog
            v-model="showScanner"
            :scanner-element-id="scannerElementId"
            @stop="stopScanner"
            @scan-file="onScanFile"
        />
    </div>
</template>

<style scoped>
.product-picker-panel {
    gap: 6px !important;
    margin-bottom: 10px !important;
    padding: 6px !important;
}

.product-picker-panel :deep(.v-field) {
    --v-input-control-height: 28px !important;
}

.product-picker-panel :deep(.v-field__input) {
    min-height: 28px !important;
    padding-top: 4px !important;
    padding-bottom: 4px !important;
}

.product-picker-panel :deep(.scanner-btn) {
    height: 30px !important;
}

.product-search-item {
    background-color: #ffffff !important;
    border: 1px solid #e2e8f0 !important;
    border-radius: 10px !important;
    margin: 6px 4px !important;
    padding: 10px 14px !important;
    transition: all 0.2s ease !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02) !important;
}

.product-search-item:hover {
    border-color: #3b82f6 !important;
    background-color: #f8fafc !important;
    transform: translateY(-1px);
    box-shadow: 0 4px 10px rgba(37, 99, 235, 0.06) !important;
}
.sp-badge,
.sku-badge {
    background-color: #e2e8f0;
    color: #475569;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 500;
}
.price-text {
    font-weight: 700;
    color: #0f172a;
    font-size: 14px;
}

.qr-reader-box {
    width: 100%;
    min-height: 320px;
    overflow: hidden;
    border: 1px solid #dbe3ef;
    border-radius: 12px;
    background: #0f172a;
}

:deep(.qr-reader-box video) {
    width: 100% !important;
    min-height: 320px;
    object-fit: cover;
}
</style>
