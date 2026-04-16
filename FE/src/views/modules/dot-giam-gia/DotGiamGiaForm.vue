<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { CalendarIcon, GiftIcon, InfoCircleIcon, TagIcon, BoxIcon, SearchIcon, TrashIcon } from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const products = ref([]); 
const selectedVariantsIds = ref([]); 
const searchQuery = ref('');

// --- PHẦN THÊM MỚI: Khởi tạo các biến lọc cho bảng dưới ---
const detailFilters = ref({
    thuongHieu: null,
    chatLieu: null,
    kichCo: null,
    mauSac: null,
    loaiSan: null,
    khoangGia: [0, 5000000] // Giá từ 0đ đến 5tr
});

const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
const isDetailView = computed(() => route.path.includes('/detail'));
const primaryColor = '#2E4E8E';

const form = ref({
    ma: '',
    ten: '',
    moTa: '',
    loaiGiamGia: 'PHAN_TRAM',
    soTienGiam: 0,
    dieuKienGiamGia: 0,
    mucUuTien: 0,
    ngayBatDau: '',
    ngayKetThuc: '',
    trangThai: 'DANG_HOAT_DONG'
});

const filteredProductsToSelect = computed(() => {
    if (!searchQuery.value) return products.value;
    const query = searchQuery.value.toLowerCase();
    return products.value.filter(p => 
        p.tenSanPhamDayDu?.toLowerCase().includes(query) || 
        p.ma?.toLowerCase().includes(query)
    );
});

const selectedProductsDetails = computed(() => {
    return products.value.filter(p => selectedVariantsIds.value.includes(p.id));
});

// --- PHẦN THÊM MỚI: Logic lọc cho bảng chi tiết phía dưới ---
const filteredSelectedDetails = computed(() => {
    return selectedProductsDetails.value.filter(p => {
        const giaSauGiam = calculateDiscountedPrice(p.giaGoc);
        
        const matchPrice = giaSauGiam >= detailFilters.value.khoangGia[0] && 
                          giaSauGiam <= detailFilters.value.khoangGia[1];
        
        const matchBrand = !detailFilters.value.thuongHieu || p.thuongHieu === detailFilters.value.thuongHieu;
        const matchSize = !detailFilters.value.kichCo || p.kichCo === detailFilters.value.kichCo;
        
        return matchPrice && matchBrand && matchSize;
    });
});

const calculateDiscountedPrice = (originalPrice) => {
    if (form.value.loaiGiamGia === 'PHAN_TRAM') {
        return originalPrice * (1 - (form.value.soTienGiam || 0) / 100);
    } else {
        return Math.max(0, originalPrice - (form.value.soTienGiam || 0));
    }
};

const formatCurrency = (value) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const toggleProduct = (id) => {
    const index = selectedVariantsIds.value.indexOf(id);
    if (index > -1) {
        selectedVariantsIds.value.splice(index, 1);
    } else {
        selectedVariantsIds.value.push(id);
    }
};

const removeSelectedProduct = (id) => {
    const index = selectedVariantsIds.value.indexOf(id);
    if (index > -1) {
        selectedVariantsIds.value.splice(index, 1);
    }
};

const removeAllSelected = () => {
    selectedVariantsIds.value = [];
};

const init = async () => {
    try {
        const data = await dichVuDotGiamGia.layDanhSachSanPhamApDung();
        products.value = (data || []).map(p => ({
            ...p,
            anhMauc: p.anhMauc || 'https://via.placeholder.com/40',
            color: p.color || 'Xanh dương',
            kichCo: p.kichCo || 41,
            loaiSan: p.loaiSan || 'Sân cỏ nhân tạo (TF)',
            giaGoc: p.giaBan || 2000000
        }));
    } catch (e) {
        console.error('Error loading products:', e);
    }

    if (isEditMode.value || isDetailView.value) {
        loading.value = true;
        try {
            const data = await dichVuDotGiamGia.layChiTietDotGiamGia(route.params.id);
            form.value = {
                ...data,
                ngayBatDau: data.ngayBatDau ? new Date(data.ngayBatDau).toISOString().slice(0, 16) : '',
                ngayKetThuc: data.ngayKetThuc ? new Date(data.ngayKetThuc).toISOString().slice(0, 16) : ''
            };
            const applied = await dichVuDotGiamGia.layDanhSachBienTheApDung(route.params.id);
            selectedVariantsIds.value = applied.map((v) => v.id);
        } catch (e) {
            addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin', color: 'error' });
        } finally {
            loading.value = false;
        }
    }
};

const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const handleSave = () => {
    if (!form.value.ten || !form.value.soTienGiam || !form.value.ngayBatDau || !form.value.ngayKetThuc || selectedVariantsIds.value.length === 0) {
        addNotification({ title: 'Thiếu thông tin', subtitle: 'Vui lòng điền đủ các trường và chọn ít nhất 1 sản phẩm', color: 'warning' });
        return;
    }
    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'Xác nhận cập nhật' : 'Xác nhận thêm mới',
        message: isEditMode.value
            ? `Bạn có chắc muốn cập nhật chiến dịch [${form.value.ten}]?`
            : `Bạn có chắc muốn tạo chiến dịch mới [${form.value.ten}]?`,
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            saving.value = true;
            try {
                const payload = {
                    ...form.value,
                    ngayBatDau: new Date(form.value.ngayBatDau).getTime(),
                    ngayKetThuc: new Date(form.value.ngayKetThuc).getTime(),
                    listIdChiTietSanPham: selectedVariantsIds.value
                };
                if (isEditMode.value) {
                    await dichVuDotGiamGia.capNhatDotGiamGia(route.params.id, payload);
                    addNotification({ title: 'Thành công', subtitle: 'Cập nhật hoàn tất', color: 'success' });
                } else {
                    await dichVuDotGiamGia.taoDotGiamGia(payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã tạo chiến dịch mới', color: 'success' });
                }
                confirmDialog.value.show = false;
                router.push('/dot-giam-gia');
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi lưu dữ liệu', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
                saving.value = false;
            }
        }
    };
};

onMounted(init);
</script>

<template>
    <v-container fluid class="pa-6 bg-grey-lighten-4 min-h-screen">
        <div class="d-flex align-center mb-6">
            <h1 class="text-h5 font-weight-bold text-uppercase" :style="{ color: primaryColor }">
                {{ isDetailView ? 'Chi tiết đợt ưu đãi' : isEditMode ? 'Cập nhật chiến dịch' : 'Thêm đợt giảm giá' }}
            </h1>
            <v-spacer></v-spacer>
            <v-btn 
                variant="flat" 
                :color="primaryColor" 
                class="text-none rounded-lg font-weight-bold" 
                prepend-icon="mdi-arrow-left"
                @click="router.push('/dot-giam-gia')"
            >
                Quay lại
            </v-btn>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
            </v-col>
        </v-row>

        <v-row v-else>
            <v-col cols="12" md="4">
                <v-card class="rounded-lg border shadow-none pa-6 mb-6">
                    <h3 class="text-subtitle-1 font-weight-bold mb-5" :style="{ color: primaryColor }">
                        Thông tin đợt giảm giá
                    </h3>
                    
                    <div class="field-group mb-4">
                        <label class="d-block text-caption font-weight-bold mb-1">Mã đợt (Tự động sinh):</label>
                        <v-text-field v-model="form.ma" readonly placeholder="Mã tự sinh (DGG-...)" variant="outlined" density="compact" hide-details class="bg-grey-lighten-5"></v-text-field>
                    </div>

                    <div class="field-group mb-4">
                        <label class="d-block text-caption font-weight-bold mb-1">Tên đợt giảm giá: *</label>
                        <v-text-field v-model="form.ten" :readonly="isDetailView" placeholder="Nhập tên đợt giảm giá" variant="outlined" density="compact" hide-details></v-text-field>
                    </div>

                    <div class="field-group mb-4">
                        <label class="d-block text-caption font-weight-bold mb-1">Giá trị giảm (%): *</label>
                        <v-text-field v-model.number="form.soTienGiam" :readonly="isDetailView" type="number" placeholder="Mời nhập giảm theo %" variant="outlined" density="compact" hide-details></v-text-field>
                    </div>

                    <div class="field-group mb-4">
                        <label class="d-block text-caption font-weight-bold mb-1">Ngày bắt đầu: *</label>
                        <v-text-field v-model="form.ngayBatDau" :readonly="isDetailView" type="datetime-local" variant="outlined" density="compact" hide-details></v-text-field>
                    </div>

                    <div class="field-group mb-6">
                        <label class="d-block text-caption font-weight-bold mb-1">Ngày kết thúc: *</label>
                        <v-text-field v-model="form.ngayKetThuc" :readonly="isDetailView" type="datetime-local" variant="outlined" density="compact" hide-details></v-text-field>
                    </div>

                    <v-btn v-if="!isDetailView" block height="48" class="text-none font-weight-bold rounded-lg" :color="primaryColor" :loading="saving" @click="handleSave">
                        + {{ isEditMode ? 'Cập nhật đợt giảm giá' : 'Tạo đợt giảm giá' }}
                    </v-btn>
                </v-card>
            </v-col>

            <v-col cols="12" md="8">
                <v-card class="rounded-lg border shadow-none pa-6 mb-6">
                    <h3 class="text-subtitle-1 font-weight-bold mb-5" :style="{ color: primaryColor }">
                        Chọn sản phẩm áp dụng
                    </h3>
                    
                    <v-row dense class="mb-4">
                        <v-col cols="12">
                            <v-text-field v-model="searchQuery" prepend-inner-icon="mdi-magnify" placeholder="Tìm theo tên hoặc mã sản phẩm..." variant="outlined" density="compact" hide-details class="mb-4"></v-text-field>
                        </v-col>
                    </v-row>

                    <v-table class="border rounded-lg custom-table to-select-table" height="300px" fixed-header>
                        <thead>
                            <tr>
                                <th class="text-center" width="60"><v-checkbox hide-details density="compact" @change="e => { if(e.target.checked) selectedVariantsIds = products.map(p => p.id); else selectedVariantsIds = [] }"></v-checkbox></th>
                                <th class="text-left">Mã SP</th>
                                <th class="text-left">Tên sản phẩm</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="(item, index) in filteredProductsToSelect" :key="item.id">
                                <td class="text-center">
                                    <v-checkbox :model-value="selectedVariantsIds.includes(item.id)" @update:model-value="toggleProduct(item.id)" :readonly="isDetailView" hide-details density="compact" :color="primaryColor"></v-checkbox>
                                </td>
                                <td class="font-weight-medium">{{ item.ma || 'SP' + String(index + 1).padStart(5, '0') }}</td>
                                <td>{{ item.tenSanPhamDayDu }}</td>
                            </tr>
                        </tbody>
                    </v-table>
                </v-card>
            </v-col>

            <v-col cols="12">
                <v-card class="rounded-lg border shadow-none pa-6">
                    <div class="d-flex align-center justify-space-between mb-5">
                        <div class="d-flex align-center">
                            <h3 class="text-subtitle-1 font-weight-bold" :style="{ color: primaryColor }">
                                Danh sách chi tiết sản phẩm được chọn
                            </h3>
                            <v-chip color="info" size="x-small" variant="flat" class="ml-2 font-weight-bold">
                                {{ selectedVariantsIds.length }}
                            </v-chip>
                        </div>
                        <v-btn variant="outlined" color="error" size="small" class="text-none rounded-lg" prepend-icon="mdi-trash-can-outline" @click="removeAllSelected">
                            Xóa tất cả
                        </v-btn>
                    </div>

                    <v-row dense class="mb-4">
                        <v-col cols="12" sm="2">
                            <v-select label="-- Thương hiệu --" v-model="detailFilters.thuongHieu" :items="['Adidas', 'Nike']" density="compact" variant="outlined" hide-details></v-select>
                        </v-col>
                        <v-col cols="12" sm="2">
                            <v-select label="-- Chất liệu --" v-model="detailFilters.chatLieu" :items="['Da', 'Vải']" density="compact" variant="outlined" hide-details></v-select>
                        </v-col>
                        <v-col cols="12" sm="2">
                            <v-select label="-- Kích cỡ --" v-model="detailFilters.kichCo" :items="[39, 40, 41, 42]" density="compact" variant="outlined" hide-details></v-select>
                        </v-col>
                        <v-col cols="12" sm="2">
                            <v-select label="-- Màu sắc --" v-model="detailFilters.mauSac" :items="['Xanh', 'Đỏ']" density="compact" variant="outlined" hide-details></v-select>
                        </v-col>
                        <v-col cols="12" sm="3">
                            <v-select label="-- Loại sản --" v-model="detailFilters.loaiSan" :items="['TF', 'FG']" density="compact" variant="outlined" hide-details></v-select>
                        </v-col>
                        <v-col cols="auto" class="d-flex align-center">
                             <v-btn icon variant="text" color="grey-darken-1" size="small"><v-icon>mdi-filter-variant</v-icon></v-btn>
                        </v-col>
                    </v-row>

                    <div class="price-slider-container mb-6 px-2">
                        <div class="d-flex align-center mb-1">
                            <v-icon size="18" class="mr-2">mdi-tune-variant</v-icon>
                            <span class="text-caption font-weight-bold">Khoảng giá</span>
                            <v-spacer></v-spacer>
                            <span class="text-caption font-weight-bold">{{ formatCurrency(detailFilters.khoangGia[0]) }} - {{ formatCurrency(detailFilters.khoangGia[1]) }}</span>
                        </div>
                        <v-range-slider
                            v-model="detailFilters.khoangGia"
                            :max="5000000"
                            :min="0"
                            :step="50000"
                            hide-details
                            color="red-darken-1"
                            track-color="black"
                            thumb-size="14"
                        ></v-range-slider>
                    </div>

                    <v-table class="border rounded-lg custom-table detailed-selected-table">
                        <thead>
                            <tr>
                                <th class="text-center" width="50">#</th>
                                <th class="text-center" width="60">Ảnh</th>
                                <th class="text-left">Mã SP (CT)</th>
                                <th class="text-left">Tên sản phẩm</th>
                                <th class="text-right">Giá bán</th>
                                <th class="text-left">Màu sắc</th>
                                <th class="text-center">Kích cỡ</th>
                                <th class="text-left">Loại Sân</th>
                                <th class="text-center" width="60">Xóa</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="(item, index) in filteredSelectedDetails" :key="item.id">
                                <td class="text-center text-grey-darken-1">{{ index + 1 }}</td>
                                <td class="text-center py-2">
                                    <v-img :src="item.anhMauc" width="40" height="40" cover class="rounded-lg border"></v-img>
                                </td>
                                <td class="font-weight-medium text-blue-darken-3">{{ item.ma }}</td>
                                <td>{{ item.tenSanPhamDayDu }}</td>
                                <td class="text-right">
                                    <div class="text-caption text-grey text-decoration-line-through">{{ formatCurrency(item.giaGoc) }}</div>
                                    <div class="font-weight-bold text-error">{{ formatCurrency(calculateDiscountedPrice(item.giaGoc)) }}</div>
                                </td>
                                <td>
                                    <div class="d-flex align-center gap-2">
                                        <div class="color-dot" :style="{ backgroundColor: item.color === 'Xanh dương' ? '#2196F3' : '#FFC107' }"></div>
                                        {{ item.color }}
                                    </div>
                                </td>
                                <td class="text-center font-weight-medium">{{ item.kichCo }}</td>
                                <td class="text-caption text-grey-darken-1">{{ item.loaiSan }}</td>
                                <td class="text-center">
                                    <v-btn icon variant="text" color="error" size="small" @click="removeSelectedProduct(item.id)">
                                        <TrashIcon size="18" />
                                    </v-btn>
                                </td>
                            </tr>
                            <tr v-if="filteredSelectedDetails.length === 0">
                                <td colspan="9" class="text-center py-10 text-grey bg-grey-lighten-5">
                                    Không tìm thấy sản phẩm nào phù hợp.
                                </td>
                            </tr>
                        </tbody>
                    </v-table>
                </v-card>
            </v-col>
        </v-row>

        <AdminConfirm v-model:show="confirmDialog.show" v-bind="confirmDialog" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
.custom-table :deep(th) {
    background-color: #f8fafc !important;
    font-weight: 700 !important;
    color: #475569 !important;
    border-bottom: 1px solid #e2e8f0 !important;
}

.custom-table :deep(td) {
    border-bottom: 1px solid #f1f5f9 !important;
    font-size: 0.9rem;
}

.color-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
}

.gap-2 { gap: 8px; }

/* Custom slider style to match the photo */
:deep(.v-slider-track__fill) {
    background-color: black !important;
}
:deep(.v-slider-thumb) {
    color: #d32f2f !important;
}
</style>