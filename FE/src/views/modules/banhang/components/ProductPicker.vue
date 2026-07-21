<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { BoxIcon, XIcon } from 'vue-tabler-icons';
import { Html5Qrcode } from 'html5-qrcode';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import {
    dichVuThuongHieu,
    dichVuMucDichChay,
    dichVuMauSac,
    dichVuKichThuoc
} from '@/services/product/dichVuThuocTinh';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import { isActiveStatus } from '@/utils/statusUtils';
import { useBanHangStore } from '@/stores/banHangStore';

const emit = defineEmits(['add-product']);
const props = defineProps({
    activeOrder: {
        type: Object,
        default: null
    }
});
const store = useBanHangStore();
const { addNotification } = useNotifications();

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

// Data loading
const fetchProductSearchResults = async (keyword) => {
    store.productSearchLoading = true;
    try {
        let minGia = undefined;
        let maxGia = undefined;
        if (store.filterKhoangGia && store.filterKhoangGia !== 'ALL') {
            const option = priceRangeOptions.value.find(o => o.value === store.filterKhoangGia);
            if (option) {
                minGia = option.min;
                maxGia = option.max;
            }
        }
        const params = {
            keyword: keyword || '',
            thuongHieu: store.filterThuongHieu,
            mucDich: store.filterMucDich,
            mauSac: store.filterMauSac,
            kichCo: store.filterKichCo,
            minGia: minGia,
            maxGia: maxGia
        };
        const res = await dichVuDonHang.searchSanPham(params);
        store.productSearchResults = res || [];
    } catch (e) {
        console.error('Lỗi khi tải sản phẩm:', e);
    } finally {
        store.productSearchLoading = false;
    }
};

const loadFilterOptions = async () => {
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

        store.filterBrands = [{ title: 'Thương hiệu', value: 'ALL' }, ...pick(th).map(x => ({ title: x.ten, value: x.ten }))];
        store.filterPurposes = [{ title: 'Mục đích', value: 'ALL' }, ...pick(md).map(x => ({ title: x.ten, value: x.ten }))];
        store.filterColors = [{ title: 'Màu sắc', value: 'ALL' }, ...pick(ms).map(x => ({ title: x.ten, value: x.ten }))];
        store.filterSizes = [{ title: 'Kích cỡ', value: 'ALL' }, ...pick(kt).map(x => ({ title: x.ten, value: x.ten }))];

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
    return store.productSearchResults || [];
});

const onFilterChange = () => {
    store.showProductAutocomplete = true;
};

const resetFilters = () => {
    store.resetFilters();
};

let searchDebounce = null;
watch([
    () => store.productSearchKeyword,
    () => store.filterThuongHieu,
    () => store.filterMucDich,
    () => store.filterKhoangGia,
    () => store.filterMauSac,
    () => store.filterKichCo
], () => {
    if (searchDebounce) clearTimeout(searchDebounce);
    searchDebounce = setTimeout(() => {
        fetchProductSearchResults(store.productSearchKeyword);
    }, 300);
});

const onProductSearchFocus = () => {
    store.showProductAutocomplete = true;
    fetchProductSearchResults(store.productSearchKeyword);
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

const onScanSuccess = async (decodedText) => {
    await stopScanner();
    const keyword = decodedText?.trim();
    if (!keyword) return;
    // Hóa đơn đang chọn được quản lý ở BanHang.vue; dùng prop để quét QR không bị lệch state Pinia.
    if (!props.activeOrder?.id) {
        addNotification({ title: 'Chưa có hóa đơn', subtitle: 'Vui lòng tạo hoặc chọn hóa đơn trước khi quét mã.', color: 'warning' });
        return;
    }

    try {
        const variants = await dichVuDonHang.searchSanPham({ keyword });
        if (variants && variants.length > 0) {
            const normalizedKeyword = keyword.toLowerCase();
            const exactMatch = variants.find((v) => String(v.maChiTietSanPham || '').trim().toLowerCase() === normalizedKeyword) || variants[0];

            if (exactMatch) {
                if (exactMatch.trangThai !== undefined && !isActiveStatus(exactMatch.trangThai)) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã ngừng bán', color: 'error' });
                    return;
                }
                const currentStock = Number(exactMatch.soLuongTon ?? exactMatch.soLuong ?? 0);
                if (currentStock <= 0) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã hết hàng', color: 'error' });
                    return;
                }
                emit('add-product', { ...exactMatch, _soLuongMuonThem: 1 });
            }
        } else {
            addNotification({ title: 'Không tìm thấy', subtitle: `Không tìm thấy mã sản phẩm ${keyword}`, color: 'warning' });
        }
    } catch (e) {
        console.error('Scan error:', e);
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
    <div class="d-flex flex-column ga-2 mb-3 bg-slate-50 pa-2 rounded-xl w-100">
        <!-- Hàng 1: Các Combobox lọc (CBO) lên trên -->
        <div class="d-flex align-center ga-2 flex-wrap">
            <!-- Thương hiệu -->
            <div style="min-width: 130px; flex: 1 1 130px;">
                <v-select v-model="store.filterThuongHieu" :items="store.filterBrands" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    bg-color="white" class="compact-select" :menu-props="{ contentClass: 'pos-select-menu' }" @update:model-value="onFilterChange" />
            </div>

            <!-- Mục đích chạy -->
            <div style="min-width: 130px; flex: 1 1 130px;">
                <v-select v-model="store.filterMucDich" :items="store.filterPurposes" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    bg-color="white" class="compact-select" :menu-props="{ contentClass: 'pos-select-menu' }" @update:model-value="onFilterChange" />
            </div>

            <!-- Khoảng giá -->
            <div style="min-width: 135px; flex: 1 1 135px;">
                <v-select v-model="store.filterKhoangGia" :items="priceRangeOptions" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    bg-color="white" class="compact-select" :menu-props="{ contentClass: 'pos-select-menu' }" @update:model-value="onFilterChange" />
            </div>

            <!-- Màu sắc -->
            <div style="min-width: 110px; flex: 1 1 110px;">
                <v-select v-model="store.filterMauSac" :items="store.filterColors" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    bg-color="white" class="compact-select" :menu-props="{ contentClass: 'pos-select-menu' }" @update:model-value="onFilterChange" />
            </div>

            <!-- Kích cỡ -->
            <div style="min-width: 110px; flex: 1 1 110px;">
                <v-select v-model="store.filterKichCo" :items="store.filterSizes" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    bg-color="white" class="compact-select" :menu-props="{ contentClass: 'pos-select-menu' }" @update:model-value="onFilterChange" />
            </div>
        </div>

        <!-- Hàng 2: Input tìm kiếm và các nút -->
        <div class="d-flex align-center ga-2">
            <!-- Custom Search Input with floating Dropdown -->
            <div class="position-relative flex-grow-1">
                <v-text-field v-model="store.productSearchKeyword" placeholder="Nhập mã, tên sản phẩm"
                    variant="outlined" density="compact" hide-details prepend-inner-icon="mdi-magnify"
                    @focus="onProductSearchFocus" @click="onProductSearchFocus"
                    @blur="onProductSearchBlur" bg-color="white" class="search-input"
                    autocomplete="off" />

                <!-- Search dropdown overlay -->
                <v-card v-if="store.showProductAutocomplete && filteredProductSearchResults.length > 0"
                    class="position-absolute mt-1 elevation-4 border rounded-lg overflow-y-auto product-dropdown-card"
                    style="max-height: 495px !important; z-index: 1000; width: 100%;">
                    <v-list class="pa-0">
                        <v-list-item v-for="variant in filteredProductSearchResults" :key="variant.id"
                            @mousedown="selectProductFromSearch(variant)"
                            class="border-b py-3 hover-autocomplete-item pointer">
                            <div class="d-flex justify-space-between w-100 align-start">
                                <!-- Left info block -->
                                <div class="d-flex align-start flex-grow-1">
                                    <v-avatar rounded="lg" size="48"
                                        class="mr-3 bg-grey-lighten-4 border flex-shrink-0">
                                        <v-img v-if="variant.hinhAnh" :src="variant.hinhAnh" cover />
                                        <BoxIcon v-else size="20" class="text-grey" />
                                    </v-avatar>
                                    <div class="d-flex flex-column" style="gap: 8px !important;">
                                        <div class="text-slate-700"
                                            style="font-size: 13.5px !important; line-height: 1.3;">
                                            {{ variant.tenSanPham }}
                                        </div>
                                        <div class="d-flex align-center ga-2 mt-1 flex-wrap">
                                            <span class="sp-badge">Mã Sản phẩm: {{ variant.maSanPham ||
                                                'SP0001' }}</span>
                                            <span
                                                style="margin-left: 15px; margin-right: 15px; font-size: 11px; color: #cbd5e1; opacity: 0.4;">|</span>
                                            <span class="sku-badge">{{ variant.maChiTietSanPham
                                            }}</span>
                                        </div>
                                        <div class="d-flex align-center mt-1 text-slate-600"
                                            style="font-size: 12px; flex-wrap: wrap;">
                                            <span>Màu sắc: <span class="text-slate-500">{{
                                                variant.tenMauSac || 'Không màu' }}</span></span>
                                            <span
                                                style="margin-left: 15px; margin-right: 15px; color: #cbd5e1; opacity: 0.4;">|</span>
                                            <span>Size: <span class="text-slate-500">{{
                                                variant.tenKichThuoc || 'N/A' }}</span></span>
                                            <span
                                                style="margin-left: 15px; margin-right: 15px; color: #cbd5e1; opacity: 0.4;">|</span>
                                            <span>Tồn: <span class="text-slate-500">{{
                                                variant.soLuongTon || 0 }}</span></span>
                                        </div>
                                    </div>
                                </div>

                                <!-- Right info block -->
                                <div class="text-right flex-shrink-0 pl-3">
                                    <template v-if="variant.phanTramGiam > 0">
                                        <div class="price-text">{{ formatCurrency(variant.giaBan) }}
                                        </div>
                                        <span
                                            style="text-decoration: line-through; text-decoration-color: #94a3b8; -webkit-text-decoration-color: #94a3b8; color: #c92c04 !important; font-size: 11px !important; font-weight: normal; display: block; margin-top: 2px;">
                                            {{ formatCurrency(variant.giaBan / (1 - variant.phanTramGiam
                                                / 100)) }}
                                        </span>
                                    </template>
                                    <template v-else>
                                        <div class="price-text">{{ formatCurrency(variant.giaBan) }}
                                        </div>
                                    </template>
                                </div>
                            </div>
                        </v-list-item>
                    </v-list>
                </v-card>
                <v-card v-else-if="store.showProductAutocomplete && !store.productSearchLoading"
                    class="position-absolute w-100 mt-1 elevation-4 border rounded-lg pa-4 text-center text-grey"
                    style="z-index: 1000">
                    Không tìm thấy sản phẩm phù hợp.
                </v-card>
            </div>

            <!-- Scanner Button -->
            <v-tooltip text="Quét mã vạch" location="top" open-delay="0"
                content-class="custom-white-tooltip">
                <template v-slot:activator="{ props }">
                    <v-btn color="primary" variant="outlined" class="scanner-btn text-none px-1"
                        style="width: 44px; min-width: 44px; flex-shrink: 0;" @click="startScanner"
                        v-bind="props">
                        <v-icon>mdi-barcode-scan</v-icon>
                    </v-btn>
                </template>
            </v-tooltip>

            <!-- Reset Filters Button -->
            <v-tooltip text="Làm mới bộ lọc" location="top" open-delay="0"
                content-class="custom-white-tooltip">
                <template v-slot:activator="{ props }">
                    <v-btn color="error" variant="outlined" class="scanner-btn text-none px-1"
                        style="width: 44px; min-width: 44px; flex-shrink: 0;" @click="resetFilters"
                        v-bind="props">
                        <v-icon>mdi-filter-off</v-icon>
                    </v-btn>
                </template>
            </v-tooltip>
        </div>
        
        <!-- QR Scanner Dialog -->
        <v-dialog v-model="showScanner" max-width="500" transition="dialog-bottom-transition">
            <v-card class="rounded-xl pa-4">
                <div class="d-flex justify-space-between align-center mb-4">
                    <span class="text-h6 font-weight-bold">Quét mã sản phẩm</span>
                    <v-btn icon variant="text" @click="() => stopScanner()">
                        <XIcon />
                    </v-btn>
                </div>
                <div :id="scannerElementId" class="qr-reader-box"></div>
                <div class="mt-4 text-center text-caption text-grey">Đưa mã QR hoặc Barcode của sản phẩm vào khung hình</div>
            </v-card>
        </v-dialog>
    </div>
</template>

<style scoped>
.hover-autocomplete-item:hover {
    background-color: #f8fafc;
}
.sp-badge, .sku-badge {
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
