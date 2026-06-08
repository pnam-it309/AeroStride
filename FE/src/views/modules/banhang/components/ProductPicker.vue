<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: ProductPicker
 * Chức năng: Tìm kiếm và thêm sản phẩm vào giỏ hàng. Hỗ trợ tìm theo tên, mã, 
 *            lọc theo thuộc tính, và quét mã vạch (Barcode/QR code).
 */
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { BoxIcon, XIcon } from 'vue-tabler-icons';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { useNotifications } from '@/services/notificationService';
import { isActiveStatus } from '@/utils/statusUtils';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminFilter from '@/components/common/AdminFilter.vue';
import ProductVariantModal from './ProductVariantModal.vue';
import {
    dichVuDanhMuc,
    dichVuMauSac,
    dichVuChatLieu,
    dichVuKichThuoc,
    dichVuDeGiay,
    dichVuThuongHieu
} from '@/services/product/dichVuThuocTinh';

const props = defineProps({
    activeOrderId: String,
    loadingExternal: Boolean,
    orders: {
        type: Array,
        default: () => []
    }
});
const emit = defineEmits(['add-product']);
const { addNotification } = useNotifications();

const keyword = ref('');
const results = ref([]);
const loading = ref(false);
const hasSearched = ref(false);

const page = ref(1);
const pageSize = ref(10);

const filters = ref({
    danhMuc: 'ALL',
    mauSac: 'ALL',
    chatLieu: 'ALL',
    kichCo: 'ALL',
    deGiay: 'ALL',
    thuongHieu: 'ALL'
});

const filterOptions = ref({
    danhMuc: [],
    mauSac: [],
    chatLieu: [],
    kichCo: [],
    deGiay: [],
    thuongHieu: []
});

// Tải dữ liệu các tùy chọn bộ lọc (danh mục, màu sắc, chất liệu...) từ API
const loadFilterOptions = async () => {
    try {
        const [dm, ms, cl, kc, dg, th] = await Promise.all([
            dichVuDanhMuc.layDanhMuc({ size: 1000 }),
            dichVuMauSac.layMauSac({ size: 1000 }),
            dichVuChatLieu.layChatLieu({ size: 1000 }),
            dichVuKichThuoc.layKichThuoc({ size: 1000 }),
            dichVuDeGiay.layDeGiay({ size: 1000 }),
            dichVuThuongHieu.layThuongHieu({ size: 1000 })
        ]);

        const pick = (res) => res?.content || res || [];
        filterOptions.value = {
            danhMuc: pick(dm).map((x) => x?.ten).filter(Boolean),
            mauSac: pick(ms).map((x) => x?.ten).filter(Boolean),
            chatLieu: pick(cl).map((x) => x?.ten).filter(Boolean),
            kichCo: pick(kc).map((x) => x?.ten).filter(Boolean),
            deGiay: pick(dg).map((x) => x?.ten).filter(Boolean),
            thuongHieu: pick(th).map((x) => x?.ten).filter(Boolean)
        };
    } catch (e) {
        console.error('Load filter options failed:', e);
    }
};

let debounceTimer = null;
watch(keyword, (newVal) => {
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        onSearch();
    }, 400);
});

// Chuyển đổi danh sách thuộc tính thành mảng các lựa chọn { title, value } cho <v-select>
const mapFilterItems = (options) => {
    return [
        { title: 'Tất cả', value: 'ALL' },
        ...Array.from(new Set(options || [])).map((opt) => ({ title: opt, value: opt }))
    ];
};

const getVariantStock = (variant) => Number(variant?.soLuongTon ?? variant?.soLuong ?? 0);

const groupVariantsByProduct = (items) => {
    const groups = new Map();

    (items || []).forEach((variant) => {
        const key = variant.maSanPham || variant.tenSanPham || variant.id;
        if (!groups.has(key)) {
            groups.set(key, {
                id: key,
                maSanPham: variant.maSanPham,
                tenSanPham: variant.tenSanPham,
                tenDanhMuc: variant.tenDanhMuc,
                tenThuongHieu: variant.tenThuongHieu,
                tenChatLieu: variant.tenChatLieu,
                tenDeGiay: variant.tenDeGiay,
                hinhAnh: variant.hinhAnh,
                variants: []
            });
        }

        groups.get(key).variants.push({
            ...variant,
            soLuong: getVariantStock(variant)
        });
    });

    return Array.from(groups.values()).map((group) => {
        const prices = group.variants.map((variant) => Number(variant.giaBan || 0)).filter((price) => price >= 0);
        return {
            ...group,
            tongSoLuongTon: group.variants.reduce((sum, variant) => sum + getVariantStock(variant), 0),
            giaBanThapNhat: prices.length ? Math.min(...prices) : null,
            giaBanCaoNhat: prices.length ? Math.max(...prices) : null
        };
    });
};

// Gửi yêu cầu tìm kiếm sản phẩm tới server dựa trên keyword.
// Dùng endpoint bán hàng để lấy trực tiếp biến thể hợp lệ và tồn kho đã trừ realtime từ DB.
const onSearch = async () => {
    if (debounceTimer) clearTimeout(debounceTimer);
    hasSearched.value = true;
    loading.value = true;
    try {
        const response = await dichVuDonHang.searchSanPham(keyword.value?.trim() || '');
        results.value = groupVariantsByProduct(Array.isArray(response) ? response : []);
        page.value = 1;
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
};

// QR / Barcode Scanner Logic
const showScanner = ref(false);
let html5QrcodeScanner = null;

// Khởi động camera quét mã vạch/QR bằng html5-qrcode
const startScanner = () => {
    showScanner.value = true;
    setTimeout(() => {
        const el = document.getElementById('reader');
        if (!el || el.clientWidth === 0) {
            startScanner();
            return;
        }

        if (html5QrcodeScanner) {
            html5QrcodeScanner.clear().catch(e => console.error(e));
        }

        html5QrcodeScanner = new Html5QrcodeScanner('reader', { fps: 10, qrbox: { width: 250, height: 250 } }, /* verbose= */ false);
        html5QrcodeScanner.render(onScanSuccess, onScanFailure);
    }, 150);
};

// Dừng camera và đóng hộp thoại quét mã
const stopScanner = () => {
    if (html5QrcodeScanner) {
        html5QrcodeScanner.clear().catch((error) => console.error('Failed to clear scanner', error));
        html5QrcodeScanner = null;
    }
    showScanner.value = false;
};

// Xử lý khi quét mã thành công: tìm kiếm sản phẩm theo mã và tự động thêm vào giỏ
const onScanSuccess = async (decodedText) => {
    stopScanner();
    // Validate khoảng trắng
    const keyword = decodedText?.trim();
    if (!keyword) return;
    if (!props.activeOrderId) {
        addNotification({ title: 'Chưa có hóa đơn', subtitle: 'Vui lòng tạo hoặc chọn hóa đơn trước khi quét mã.', color: 'warning' });
        return;
    }

    try {
        const variants = await dichVuDonHang.searchSanPham(keyword);
        if (variants && variants.length > 0) {
            const exactMatch = variants.find((v) => v.maChiTietSanPham === keyword) || variants[0];
            
            if (exactMatch) {
                if (exactMatch.trangThai !== undefined && !isActiveStatus(exactMatch.trangThai)) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã ngừng bán', color: 'error' });
                    return;
                }
                const currentStock = getAdjustedStock(exactMatch);
                if (currentStock <= 0) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã hết hàng', color: 'error' });
                    return;
                }
                
                // QR/barcode của biến thể được cộng trực tiếp 1 sản phẩm vào giỏ.
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
    // console.warn(`Code scan error = ${error}`);
};

// Hỗ trợ súng quét mã (Barcode Scanner giả lập bàn phím)
let barcodeBuffer = '';
let lastKeyTime = 0;

// Lắng nghe phím bấm từ bàn phím hoặc súng quét mã vạch (barcode scanner emulator)
const handleGlobalKeyDown = (e) => {
    // Nếu đang tập trung vào input thì không xử lý tự động
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

    const currentTime = new Date().getTime();
    if (currentTime - lastKeyTime > 100) {
        barcodeBuffer = '';
    }

    if (e.key === 'Enter') {
        if (barcodeBuffer.length > 3) {
            onScanSuccess(barcodeBuffer);
            barcodeBuffer = '';
        }
    } else if (e.key.length === 1) {
        barcodeBuffer += e.key;
    }
    lastKeyTime = currentTime;
};

onMounted(() => {
    window.addEventListener('keydown', handleGlobalKeyDown);
    loadFilterOptions();
    onSearch(); // Load all products by default
});

onUnmounted(() => {
    window.removeEventListener('keydown', handleGlobalKeyDown);
});

// Phát sự kiện thêm sản phẩm được chọn vào giỏ hàng
const selectProduct = (p) => {
    emit('add-product', p);
    // keep keyword/results for fast adding multiple items like the sample UX
};

// Tính toán số lượng tồn kho thực tế còn lại (trừ đi số lượng đang nằm trong các giỏ hàng chờ)
const getAdjustedStock = (p) => {
    return Math.max(0, getVariantStock(p));
};

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

// Tính toán danh sách sản phẩm sau khi áp dụng các bộ lọc ở frontend
const filteredResults = computed(() => {
    const rs = results.value || [];
    const f = filters.value;
    const match = (key, val) => {
        if (val === 'ALL') return true;
        return String(key || '') === String(val);
    };
    const anyVariantMatch = (variants, key, val) => {
        if (val === 'ALL') return true;
        return (variants || []).some((variant) => String(variant?.[key] || '') === String(val));
    };

    return rs.filter((x) => {
        const active = x.trangThai === undefined || isActiveStatus(x.trangThai);
        
        return (
            active &&
            match(x.tenDanhMuc, f.danhMuc) &&
            match(x.tenChatLieu, f.chatLieu) &&
            match(x.tenDeGiay, f.deGiay) &&
            match(x.tenThuongHieu, f.thuongHieu) &&
            anyVariantMatch(x.variants, 'tenMauSac', f.mauSac) &&
            anyVariantMatch(x.variants, 'tenKichThuoc', f.kichCo)
        );
    });
});

// Chuẩn hóa danh sách kết quả, gán variants từ chiTietSanPhams
const mappedResults = computed(() => {
    return filteredResults.value.map(p => ({
        ...p,
        variants: p.variants || []
    }));
});

// Modal trạng thái
const showVariantModal = ref(false);
const selectedProductForModal = ref(null);
const loadingVariant = ref({});

// Mở modal khi bấm "Chọn mua"
const openVariantModal = async (p) => {
    if (p.variants && p.variants.length > 0) {
        selectedProductForModal.value = p;
        showVariantModal.value = true;
        return;
    }
};

// Xử lý khi user chọn xong trong Modal
const handleAddToCartFromModal = ({ variant, quantity }) => {
    emit('add-product', { ...variant, _soLuongMuonThem: quantity });
};

const getTotalGroupStock = (p) => {
    return Number(p.tongSoLuongTon || 0);
};

const getPriceRange = (p) => {
    if (p.giaBanThapNhat == null && p.giaBanCaoNhat == null) return '--';
    if (p.giaBanThapNhat === p.giaBanCaoNhat) return formatCurrency(p.giaBanThapNhat);
    return `${formatCurrency(p.giaBanThapNhat)} - ${formatCurrency(p.giaBanCaoNhat)}`;
};

const totalElements = computed(() => mappedResults.value.length);
const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / pageSize.value)));
const pagedResults = computed(() => {
    const start = (page.value - 1) * pageSize.value;
    return mappedResults.value.slice(start, start + pageSize.value);
});

// Lấy URL hình ảnh của biến thể (hoặc ảnh gốc)
const imageSrc = (p) => {
    const src = p?.hinhAnh || p?.variants?.[0]?.hinhAnh;
    return src || null;
};

const tableHeaders = [
    { text: 'STT', width: '50px', align: 'center' },
    { text: 'Ảnh', width: '70px', align: 'center' },
    { text: 'Sản phẩm', align: 'left' },
    { text: 'Tồn kho', width: '100px', align: 'center' },
    { text: 'Đơn giá', width: '130px', align: 'right' },
    { text: 'Thao tác', width: '120px', align: 'center' }
];

defineExpose({
    refresh: onSearch
});
</script>

<template>
    <div class="product-picker">
        <!-- 1. Bộ lọc đồng bộ -->
        <AdminFilter title="Tìm kiếm & Lọc sản phẩm" :loading="loading" @refresh="onSearch">
            <!-- Tìm kiếm -->
            <v-col cols="12" md="4">
                <div class="filter-field-label">Tìm kiếm</div>
                <v-text-field v-model="keyword" placeholder="Nhập tên hoặc mã sản phẩm..." variant="outlined"
                    density="compact" hide-details bg-color="white" prepend-inner-icon="mdi-magnify"
                    @keydown.enter="onSearch" class="compact-input">
                    <template #append-inner>
                        <v-btn icon color="primary" variant="text" size="small" class="mr-n2" @click="startScanner">
                            <v-icon>mdi-qrcode-scan</v-icon>
                        </v-btn>
                    </template>
                </v-text-field>
            </v-col>

            <!-- Danh mục -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Danh mục</div>
                <v-select v-model="filters.danhMuc" :items="mapFilterItems(filterOptions.danhMuc)" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    class="compact-input"></v-select>
            </v-col>

            <!-- Thương hiệu -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Thương hiệu</div>
                <v-select v-model="filters.thuongHieu" :items="mapFilterItems(filterOptions.thuongHieu)"
                    item-title="title" item-value="value" density="compact" hide-details variant="outlined"
                    class="compact-input"></v-select>
            </v-col>

            <!-- Màu sắc -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Màu sắc</div>
                <v-select v-model="filters.mauSac" :items="mapFilterItems(filterOptions.mauSac)" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    class="compact-input"></v-select>
            </v-col>

            <!-- Chất liệu -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Chất liệu</div>
                <v-select v-model="filters.chatLieu" :items="mapFilterItems(filterOptions.chatLieu)" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    class="compact-input"></v-select>
            </v-col>

            <!-- Kích cỡ -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Kích cỡ</div>
                <v-select v-model="filters.kichCo" :items="mapFilterItems(filterOptions.kichCo)" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    class="compact-input"></v-select>
            </v-col>

            <!-- Đế giày -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Đế giày</div>
                <v-select v-model="filters.deGiay" :items="mapFilterItems(filterOptions.deGiay)" item-title="title"
                    item-value="value" density="compact" hide-details variant="outlined"
                    class="compact-input"></v-select>
            </v-col>
        </AdminFilter>

        <!-- QR Scanner Dialog -->
        <v-dialog v-model="showScanner" max-width="500" transition="dialog-bottom-transition">
            <v-card class="rounded-xl pa-4">
                <div class="d-flex justify-space-between align-center mb-4">
                    <span class="text-h6 font-weight-bold">Quét mã sản phẩm</span>
                    <v-btn icon variant="text" @click="stopScanner">
                        <XIcon />
                    </v-btn>
                </div>
                <div id="reader" style="width: 100%"></div>
                <div class="mt-4 text-center text-caption text-grey">Đưa mã QR hoặc Barcode của sản phẩm vào khung hình
                </div>
            </v-card>
        </v-dialog>

        <div class="mt-2 table-wrapper-picker">
            <AdminTable title="Kết quả tìm kiếm" :headers="tableHeaders" :items="pagedResults" :loading="loading"
                :showAddButton="false"
                class="custom-scroll-table balanced-table"
                :emptyText="hasSearched ? 'Không tìm thấy sản phẩm phù hợp' : 'Nhập từ khóa hoặc bấm TÌM KIẾM'"
                emptyIcon="mdi-magnify">
                <template #row="{ item: p, index }">
                    <tr class="data-row">
                        <td class="data-cell text-center font-weight-bold text-slate-500">
                            {{ (page - 1) * pageSize + index + 1 }}
                        </td>
                        <td class="data-cell text-center">
                            <v-avatar rounded="lg" size="56" color="grey-lighten-4">
                                <v-img v-if="imageSrc(p)" :src="imageSrc(p)" cover />
                                <BoxIcon v-else size="22" class="text-grey-darken-1" />
                            </v-avatar>
                        </td>
                        <td class="data-cell text-left">
                            <div class="font-weight-bold text-truncate" :title="p.tenSanPham">{{ p.tenSanPham }}</div>
                            <div class="text-caption text-slate-500">Mã: {{ p.maSanPham }}</div>
                        </td>
                        <td class="data-cell text-center font-weight-bold text-primary">
                            {{ getTotalGroupStock(p) }}
                        </td>
                        <td class="data-cell text-right font-weight-bold text-slate-700">
                            {{ getPriceRange(p) }}
                        </td>
                        <td class="data-cell text-center">
                            <v-btn v-if="getTotalGroupStock(p) > 0" color="blue-lighten-5" size="small" variant="flat" class="font-weight-bold text-primary rounded-lg px-4"
                                :disabled="loadingExternal || loadingVariant[p.id]"
                                :loading="loadingVariant[p.id]" @click="openVariantModal(p)">
                                Chọn mua
                            </v-btn>
                            <v-btn v-else color="grey-lighten-3" size="small" variant="flat" class="font-weight-bold text-grey-darken-1 rounded-lg px-4"
                                disabled>
                                Hết hàng
                            </v-btn>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination v-model="page" :page-size="pageSize" :total-pages="totalPages"
                        :total-elements="totalElements" :current-size="pagedResults.length"
                        @update:pageSize="pageSize = $event" />
                </template>
            </AdminTable>
        </div>

        <ProductVariantModal 
            :show="showVariantModal" 
            @update:show="showVariantModal = $event"
            :productGroup="selectedProductForModal"
            @add-to-cart="handleAddToCartFromModal"
        />
    </div>
</template>

<style scoped>
.table-wrapper-picker {
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #fff;
    overflow: hidden;
}

.filter-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #475569;
    margin-bottom: 4px;
}

.compact-input :deep(.v-field) {
    border-radius: 8px !important;
}

.product-picker :deep(.filter-card) {
    margin-bottom: 16px !important;
    background-color: #f8fafc !important;
}

@media (max-width: 960px) {
    .table-wrapper-picker {
        min-height: 280px;
    }
}

.mini-select {
    max-width: 120px;
}

.mini-select :deep(.v-field__input) {
    padding-top: 2px !important;
    padding-bottom: 2px !important;
    min-height: 28px !important;
    font-size: 12px !important;
}

.mini-select :deep(.v-field) {
    border-radius: 6px !important;
    --v-input-control-height: 28px !important;
    font-size: 12px !important;
}

/* Enable vertical scroll for table when items exceed ~10 rows */
.custom-scroll-table :deep(.table-wrapper) {
    max-height: 550px;
    overflow-y: auto;
}

.custom-scroll-table :deep(.native-admin-table thead th) {
    position: sticky;
    top: 0;
    z-index: 10;
    background-color: #f8fafc !important;
    box-shadow: inset 0 -1px 0 #e2e8f0;
}
</style>
