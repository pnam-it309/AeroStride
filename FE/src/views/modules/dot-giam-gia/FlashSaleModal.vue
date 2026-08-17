<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';
import { useNotifications } from '@/services/notificationService';
import { formatCurrency } from '@/utils/formatters';

const props = defineProps({
    modelValue: { type: Boolean, default: false }
});

const emit = defineEmits(['update:modelValue', 'success']);

const { addNotification } = useNotifications();

const dialog = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const submitting = ref(false);
const productList = ref([]);
const searchProduct = ref('');

// Presets for Time Slots
const TIME_SLOTS = [
    { label: '09:00 - 12:00', startHour: 9, startMin: 0, endHour: 12, endMin: 0, desc: 'Khởi động sáng' },
    { label: '12:00 - 14:00', startHour: 12, startMin: 0, endHour: 14, endMin: 0, desc: 'Nghỉ trưa vàng' },
    { label: '18:00 - 21:00', startHour: 18, startMin: 0, endHour: 21, endMin: 0, desc: 'Tan sở săn deal' },
    { label: '20:00 - 23:59', startHour: 20, startMin: 0, endHour: 23, endMin: 59, desc: 'Đêm muộn cuồng nhiệt' }
];

const selectedSlot = ref(TIME_SLOTS[1]); // Mặc định 12:00 - 14:00
const selectedDate = ref(new Date().toISOString().split('T')[0]); // YYYY-MM-DD
const customStartTime = ref('12:00');
const customEndTime = ref('14:00');
const isCustomSlot = ref(false);

const discountPercent = ref(30);
const campaignName = ref('');
const campaignCode = ref('');
const selectedVariantIds = ref([]);

// Preset discount options
const DISCOUNT_PRESETS = [15, 20, 30, 40, 50, 70];

const generateCampaignDetails = () => {
    const d = new Date(selectedDate.value);
    const dateFormatted = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
    const slotLabel = isCustomSlot.value ? `${customStartTime.value} - ${customEndTime.value}` : selectedSlot.value.label;
    
    campaignCode.value = `FS${Date.now().toString().slice(-6)}`;
    campaignName.value = `⚡ Flash Sale Giờ Vàng ${slotLabel} (${dateFormatted})`;
};

watch([selectedSlot, selectedDate, isCustomSlot, customStartTime, customEndTime], () => {
    generateCampaignDetails();
});

const calculateTimestamps = () => {
    const baseDate = new Date(selectedDate.value);
    let startH, startM, endH, endM;

    if (isCustomSlot.value) {
        const [sh, sm] = customStartTime.value.split(':').map(Number);
        const [eh, em] = customEndTime.value.split(':').map(Number);
        startH = sh; startM = sm;
        endH = eh; endM = em;
    } else {
        startH = selectedSlot.value.startHour;
        startM = selectedSlot.value.startMin;
        endH = selectedSlot.value.endHour;
        endM = selectedSlot.value.endMin;
    }

    const startDate = new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate(), startH, startM, 0);
    const endDate = new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate(), endH, endM, 59);

    return {
        startTime: startDate.getTime(),
        endTime: endDate.getTime(),
        slotString: isCustomSlot.value ? `${customStartTime.value} - ${customEndTime.value}` : selectedSlot.value.label
    };
};

const loadProducts = async () => {
    loading.value = true;
    try {
        const data = await dichVuDotGiamGia.layDanhSachSanPhamApDung();
        productList.value = data || [];
    } catch (e) {
        console.error('Lỗi lấy danh sách sản phẩm:', e);
    } finally {
        loading.value = false;
    }
};

watch(dialog, (open) => {
    if (open) {
        selectedDate.value = new Date().toISOString().split('T')[0];
        generateCampaignDetails();
        loadProducts();
    }
});

const filteredProducts = computed(() => {
    if (!searchProduct.value.trim()) return productList.value;
    const q = searchProduct.value.toLowerCase().trim();
    return productList.value.filter(p => 
        (p.tenSanPham && p.tenSanPham.toLowerCase().includes(q)) ||
        (p.maSanPham && p.maSanPham.toLowerCase().includes(q)) ||
        (p.tenMauSac && p.tenMauSac.toLowerCase().includes(q)) ||
        (p.tenKichThuoc && p.tenKichThuoc.toLowerCase().includes(q))
    );
});

const isAllSelected = computed(() => {
    return filteredProducts.value.length > 0 && 
        filteredProducts.value.every(p => selectedVariantIds.value.includes(p.id));
});

const toggleSelectAll = () => {
    if (isAllSelected.value) {
        const currentFilteredIds = new Set(filteredProducts.value.map(p => p.id));
        selectedVariantIds.value = selectedVariantIds.value.filter(id => !currentFilteredIds.has(id));
    } else {
        const currentIds = new Set(selectedVariantIds.value);
        filteredProducts.value.forEach(p => currentIds.add(p.id));
        selectedVariantIds.value = Array.from(currentIds);
    }
};

const selectPresetSlot = (slot) => {
    isCustomSlot.value = false;
    selectedSlot.value = slot;
};

const handleSave = async () => {
    if (!campaignName.value.trim()) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng nhập tên đợt Flash Sale', color: 'warning' });
        return;
    }

    if (!discountPercent.value || discountPercent.value <= 0 || discountPercent.value > 90) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Mức giảm giá phải từ 1% đến 90%', color: 'warning' });
        return;
    }

    if (selectedVariantIds.value.length === 0) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng chọn ít nhất 1 sản phẩm tham gia Flash Sale', color: 'warning' });
        return;
    }

    const { startTime, endTime, slotString } = calculateTimestamps();

    if (endTime <= startTime) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Thời gian kết thúc phải sau thời gian bắt đầu', color: 'warning' });
        return;
    }

    submitting.value = true;
    try {
        const payload = {
            ma: campaignCode.value,
            ten: campaignName.value,
            loaiGiamGia: 'PERCENTAGE',
            soTienGiam: discountPercent.value,
            dieuKienGiamGia: 0,
            ngayBatDau: startTime,
            ngayKetThuc: endTime,
            mucUuTien: 100, // Ưu tiên cao cho Flash Sale
            moTa: `Chương trình Flash Sale Khung Giờ Vàng ${slotString}`,
            trangThai: 'DANG_HOAT_DONG',
            isFlashSale: true,
            khungGio: slotString,
            listIdChiTietSanPham: selectedVariantIds.value
        };

        await dichVuDotGiamGia.taoDotGiamGia(payload);

        addNotification({
            title: 'Thành công',
            subtitle: `Đã kích hoạt Flash Sale Giờ Vàng [${slotString}]!`,
            color: 'success'
        });

        emit('success');
        dialog.value = false;
    } catch (e) {
        console.error('Lỗi tạo Flash Sale:', e);
        const errMsg = e.response?.data?.message || 'Không thể tạo đợt Flash Sale';
        addNotification({ title: 'Lỗi', subtitle: errMsg, color: 'error' });
    } finally {
        submitting.value = false;
    }
};
</script>

<template>
    <v-dialog v-model="dialog" max-width="920px" persistent scrollable>
        <v-card class="flash-sale-card rounded-xl overflow-hidden">
            <!-- Header -->
            <div class="flash-sale-header d-flex align-center justify-space-between px-6 py-4">
                <div class="d-flex align-center">
                    <div class="flash-icon-box mr-3">
                        <v-icon icon="mdi-flash" color="amber-darken-2" size="28" class="pulse-anim" />
                    </div>
                    <div>
                        <h3 class="text-h6 font-weight-black text-white mb-0">Thiết Lập Flash Sale Khung Giờ Vàng</h3>
                        <p class="text-caption text-amber-lighten-4 mb-0">Tạo chiến dịch giảm giá thần tốc với đồng hồ đếm ngược trên trang chủ</p>
                    </div>
                </div>
                <v-btn icon="mdi-close" variant="text" color="white" density="compact" @click="dialog = false" />
            </div>

            <!-- Body -->
            <v-card-text class="pa-6" style="max-height: 70vh;">
                <!-- 1. Chọn ngày & khung giờ -->
                <div class="section-title mb-3">
                    <v-icon icon="mdi-clock-fast" color="primary" class="mr-2" size="18" />
                    <span>1. Chọn Ngày & Khung Giờ Vàng</span>
                </div>

                <v-row class="mb-4">
                    <v-col cols="12" sm="4">
                        <label class="form-label font-weight-bold">Ngày diễn ra Flash Sale</label>
                        <v-text-field
                            v-model="selectedDate"
                            type="date"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="mt-1"
                        />
                    </v-col>

                    <v-col cols="12" sm="8">
                        <label class="form-label font-weight-bold">Khung giờ săn deal</label>
                        <div class="d-flex flex-wrap gap-2 mt-1">
                            <v-btn
                                v-for="slot in TIME_SLOTS"
                                :key="slot.label"
                                :variant="!isCustomSlot && selectedSlot.label === slot.label ? 'flat' : 'outlined'"
                                :color="!isCustomSlot && selectedSlot.label === slot.label ? 'amber-darken-3' : 'grey-darken-1'"
                                size="small"
                                class="rounded-pill font-weight-bold text-none mr-2 mb-2"
                                @click="selectPresetSlot(slot)"
                            >
                                ⚡ {{ slot.label }}
                                <span class="ml-1 text-caption opacity-80">({{ slot.desc }})</span>
                            </v-btn>
                            <v-btn
                                :variant="isCustomSlot ? 'flat' : 'outlined'"
                                :color="isCustomSlot ? 'primary' : 'grey-darken-1'"
                                size="small"
                                class="rounded-pill font-weight-bold text-none mb-2"
                                @click="isCustomSlot = true"
                            >
                                🕒 Tùy chỉnh giờ
                            </v-btn>
                        </div>
                    </v-col>
                </v-row>

                <!-- Custom time inputs if selected -->
                <v-row v-if="isCustomSlot" class="mb-4 bg-slate-50 pa-3 rounded-lg border">
                    <v-col cols="12" sm="6">
                        <label class="form-label">Giờ bắt đầu</label>
                        <v-text-field v-model="customStartTime" type="time" variant="outlined" density="compact" hide-details />
                    </v-col>
                    <v-col cols="12" sm="6">
                        <label class="form-label">Giờ kết thúc</label>
                        <v-text-field v-model="customEndTime" type="time" variant="outlined" density="compact" hide-details />
                    </v-col>
                </v-row>

                <v-divider class="my-4" />

                <!-- 2. Mức giảm giá & Thông tin chiến dịch -->
                <div class="section-title mb-3">
                    <v-icon icon="mdi-percent-outline" color="primary" class="mr-2" size="18" />
                    <span>2. Mức Giảm Giá & Tên Chiến Dịch</span>
                </div>

                <v-row class="mb-4">
                    <v-col cols="12" sm="6">
                        <label class="form-label font-weight-bold">Mức giảm giá Flash Sale (%)</label>
                        <div class="d-flex align-center gap-2 mt-1">
                            <v-text-field
                                v-model.number="discountPercent"
                                type="number"
                                min="1"
                                max="90"
                                suffix="%"
                                variant="outlined"
                                density="compact"
                                hide-details
                                style="max-width: 140px;"
                            />
                            <div class="d-flex flex-wrap gap-1 ml-2">
                                <v-chip
                                    v-for="p in DISCOUNT_PRESETS"
                                    :key="p"
                                    size="small"
                                    :color="discountPercent === p ? 'deep-orange' : 'default'"
                                    :variant="discountPercent === p ? 'flat' : 'outlined'"
                                    class="cursor-pointer font-weight-bold"
                                    @click="discountPercent = p"
                                >
                                    -{{ p }}%
                                </v-chip>
                            </div>
                        </div>
                    </v-col>

                    <v-col cols="12" sm="6">
                        <label class="form-label font-weight-bold">Tên chiến dịch Flash Sale</label>
                        <v-text-field
                            v-model="campaignName"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="mt-1"
                        />
                    </v-col>
                </v-row>

                <v-divider class="my-4" />

                <!-- 3. Chọn sản phẩm áp dụng Flash Sale -->
                <div class="d-flex align-center justify-space-between mb-3">
                    <div class="section-title mb-0">
                        <v-icon icon="mdi-shoe-sneaker" color="primary" class="mr-2" size="18" />
                        <span>3. Chọn Sản Phẩm Tham Gia Flash Sale</span>
                        <v-chip size="x-small" color="primary" class="ml-2 font-weight-bold">
                            Đã chọn: {{ selectedVariantIds.length }} sản phẩm
                        </v-chip>
                    </div>
                    <v-text-field
                        v-model="searchProduct"
                        placeholder="Tìm theo tên giày, size, màu..."
                        prepend-inner-icon="mdi-magnify"
                        variant="outlined"
                        density="compact"
                        hide-details
                        clearable
                        style="max-width: 280px;"
                    />
                </div>

                <!-- Product Table -->
                <div class="product-select-table-wrapper border rounded-lg overflow-hidden">
                    <div v-if="loading" class="text-center py-8">
                        <v-progress-circular indeterminate color="primary" size="32" />
                        <div class="mt-2 text-caption text-slate-500">Đang tải danh sách sản phẩm...</div>
                    </div>

                    <table v-else class="w-100 product-table">
                        <thead class="bg-slate-100">
                            <tr>
                                <th style="width: 44px;" class="text-center">
                                    <v-checkbox-btn
                                        :model-value="isAllSelected"
                                        @update:model-value="toggleSelectAll"
                                        color="primary"
                                        density="compact"
                                    />
                                </th>
                                <th class="text-left py-2 font-weight-bold text-caption text-slate-700">Sản phẩm / Biến thể</th>
                                <th class="text-center py-2 font-weight-bold text-caption text-slate-700">Tồn kho</th>
                                <th class="text-right py-2 font-weight-bold text-caption text-slate-700">Giá gốc</th>
                                <th class="text-right py-2 font-weight-bold text-caption text-deep-orange">Giá Flash Sale (-{{ discountPercent }}%)</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr
                                v-for="item in filteredProducts"
                                :key="item.id"
                                :class="{ 'selected-row': selectedVariantIds.includes(item.id) }"
                                @click="() => {
                                    const idx = selectedVariantIds.indexOf(item.id);
                                    if (idx >= 0) selectedVariantIds.splice(idx, 1);
                                    else selectedVariantIds.push(item.id);
                                }"
                                class="cursor-pointer border-b hover:bg-slate-50"
                            >
                                <td class="text-center" @click.stop>
                                    <v-checkbox-btn
                                        v-model="selectedVariantIds"
                                        :value="item.id"
                                        color="primary"
                                        density="compact"
                                    />
                                </td>
                                <td class="py-2">
                                    <div class="font-weight-bold text-body-2 text-slate-900">{{ item.tenSanPham }}</div>
                                    <div class="text-caption text-slate-500">
                                        Màu: {{ item.tenMauSac || 'N/A' }} | Size: {{ item.tenKichThuoc || 'N/A' }}
                                    </div>
                                </td>
                                <td class="text-center py-2">
                                    <v-chip size="x-small" :color="(item.soLuongTon || item.soLuong) > 0 ? 'success' : 'error'" variant="tonal">
                                        {{ (item.soLuongTon || item.soLuong) || 0 }} đôi
                                    </v-chip>
                                </td>
                                <td class="text-right py-2 font-weight-medium text-slate-500">
                                    {{ formatCurrency(item.giaBan) }}
                                </td>
                                <td class="text-right py-2 font-weight-bold text-deep-orange">
                                    {{ formatCurrency(item.giaBan * (100 - discountPercent) / 100) }}
                                </td>
                            </tr>
                            <tr v-if="filteredProducts.length === 0">
                                <td colspan="5" class="text-center py-6 text-slate-400">
                                    Không tìm thấy sản phẩm phù hợp
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </v-card-text>

            <!-- Actions -->
            <v-divider />
            <v-card-actions class="px-6 py-3 bg-slate-50 d-flex justify-end gap-2">
                <v-btn variant="outlined" color="grey-darken-1" @click="dialog = false">
                    Hủy bỏ
                </v-btn>
                <v-btn
                    color="amber-darken-3"
                    variant="flat"
                    prepend-icon="mdi-flash"
                    class="font-weight-bold text-white px-6"
                    :loading="submitting"
                    @click="handleSave"
                >
                    ⚡ Kích Hoạt Flash Sale Ngay
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.flash-sale-header {
    background: linear-gradient(135deg, #b45309 0%, #d97706 50%, #ea580c 100%);
}

.flash-icon-box {
    width: 44px;
    height: 44px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid rgba(255, 255, 255, 0.3);
}

.section-title {
    font-size: 0.95rem;
    font-weight: 700;
    color: #1e293b;
    display: flex;
    align-items: center;
}

.form-label {
    font-size: 0.85rem;
    color: #334155;
}

.product-select-table-wrapper {
    max-height: 280px;
    overflow-y: auto;
}

.product-table {
    border-collapse: collapse;
}

.product-table th, .product-table td {
    padding: 8px 12px;
}

.selected-row {
    background-color: #fffbeb !important;
}

.pulse-anim {
    animation: pulse 1.5s infinite;
}

@keyframes pulse {
    0% { transform: scale(1); filter: drop-shadow(0 0 2px #fbbf24); }
    50% { transform: scale(1.15); filter: drop-shadow(0 0 8px #f59e0b); }
    100% { transform: scale(1); filter: drop-shadow(0 0 2px #fbbf24); }
}
</style>
