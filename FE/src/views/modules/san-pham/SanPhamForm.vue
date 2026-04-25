<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import {
    ChevronLeftIcon, DeviceFloppyIcon, PlusIcon, TrashIcon,
    PhotoIcon, InfoCircleIcon, BoxIcon, SettingsIcon, ArrowLeftIcon
} from 'vue-tabler-icons';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import {
    dichVuThuongHieu, dichVuDanhMuc, dichVuXuatXu,
    dichVuChatLieu, dichVuDeGiay, dichVuCoGiay,
    dichVuMucDichChay
} from '@/services/product/dichVuThuocTinh';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);

const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});
const isEditMode = computed(() => !!route.params.id);

// DATA OPTIONS
const brands = ref([]);
const categories = ref([]);
const materials = ref([]);
const soles = ref([]);
const collars = ref([]);
const origins = ref([]);
const purposes = ref([]);
const colors = ref([]);
const sizes = ref([]);


const onKeyUpEnter = (event, field, service, type, label) => {
    const val = event.target.value?.trim();
    if (!val) return;

    const lists = {
        'THUONG_HIEU': brands.value,
        'DANH_MUC': categories.value,
        'XUAT_XU': origins.value,
        'CHAT_LIEU': materials.value,
        'DE_GIAY': soles.value,
        'CO_GIAY': collars.value,
        'MUC_DICH_CHAY': purposes.value
    };
    const list = lists[type] || [];
    const exists = list.some(item => item.ten?.toLowerCase() === val.toLowerCase());

    if (!exists) {
        autoCreateAttribute(val, field, service, type, label);
    }
};

const fetchFormOptions = async () => {
    try {
        // Try aggregate endpoint first
        const opts = await dichVuSanPham.layOptionsForm().catch(() => null);
        if (opts) {
            brands.value = opts.thuongHieus || [];
            categories.value = opts.danhMucs || [];
            origins.value = opts.xuatXus || [];
            purposes.value = opts.mucDichChays || [];
            collars.value = opts.coGiays || [];
            materials.value = opts.chatLieus || [];
            soles.value = opts.deGiays || [];
        } else {
            // Fallback: fetch individually if form-options fails
            const [b, c, o, p, col, m, s] = await Promise.all([
                dichVuThuongHieu.layThuongHieu({ size: 1000 }),
                dichVuDanhMuc.layDanhMuc({ size: 1000 }),
                dichVuXuatXu.layXuatXu({ size: 1000 }),
                dichVuMucDichChay.layMucDichChay({ size: 1000 }),
                dichVuCoGiay.layCoGiay({ size: 1000 }),
                dichVuChatLieu.layChatLieu({ size: 1000 }),
                dichVuDeGiay.layDeGiay({ size: 1000 })
            ]);
            brands.value = b.content || b || [];
            categories.value = c.content || c || [];
            origins.value = o.content || o || [];
            purposes.value = p.content || p || [];
            collars.value = col.content || col || [];
            materials.value = m.content || m || [];
            soles.value = s.content || s || [];
        }
    } catch (error) {
        console.error('Lỗi khi tải options:', error);
        addNotification({ title: 'Cảnh báo', subtitle: 'Không thể tải đầy đủ thuộc tính', color: 'warning' });
    }
};

const autoCreateAttribute = async (val, field, service, type, label) => {
    try {
        const payload = { ten: val, ma: '', moTa: `Tự động thêm từ sản phẩm` };
        const methodName = `tao${type.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')}`;
        const newEntity = await service[methodName](payload);

        // Refresh options using the resilient fetcher
        await fetchFormOptions();

        // Select the new entity
        product.value[field] = newEntity.id;
        addNotification({ title: 'Thành công', subtitle: `Đã thêm nhanh ${label}: ${val}`, color: 'success' });
    } catch (error) {
        console.error(error);
        addNotification({ title: 'Lỗi', subtitle: `Không thể thêm nhanh ${label}`, color: 'error' });
        product.value[field] = null;
    }
};

// FORM STATE
const product = ref({
    maSanPham: '',
    tenSanPham: '',
    moTa: '',
    idThuongHieu: null,
    idDanhMuc: null,
    idXuatXu: null,
    idChatLieu: null,
    idDeGiay: null,
    idCoGiay: null,
    idMucDichChay: null,
    gioiTinhKhachHang: 'UNISEX',
    trangThai: 'DANG_HOAT_DONG',
    moTaNgan: '',
    moTaChiTiet: ''
});

const loadProduct = async (id) => {
    try {
        const data = await dichVuSanPham.layChiTietSanPham(id);
        product.value = { ...data };
    } catch (error) {
        throw error;
    }
};

onMounted(async () => {
    loading.value = true;
    try {
        await fetchFormOptions();
        // colors and sizes might still need specific handling if not in aggregate
        // Assuming they are available or handled elsewhere

        if (route.params.id) {
            await loadProduct(route.params.id);
        }
    } catch (error) {
        console.error('Error initializing form:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải dữ liệu initialize', color: 'error' });
    } finally {
        loading.value = false;
    }
});

const rules = {
    required: value => !!value || 'Trường này là bắt buộc',
    min0: value => value >= 0 || 'Giá trị không được nhỏ hơn 0',
    sku: value => !!value || 'SKU không được để trống'
};

const attributeConfig = [
    { field: 'idThuongHieu', service: dichVuThuongHieu, type: 'THUONG_HIEU', label: 'thương hiệu' },
    { field: 'idDanhMuc', service: dichVuDanhMuc, type: 'DANH_MUC', label: 'danh mục' },
    { field: 'idXuatXu', service: dichVuXuatXu, type: 'XUAT_XU', label: 'xuất xứ' },
    { field: 'idChatLieu', service: dichVuChatLieu, type: 'CHAT_LIEU', label: 'chất liệu' },
    { field: 'idDeGiay', service: dichVuDeGiay, type: 'DE_GIAY', label: 'đế giày' },
    { field: 'idCoGiay', service: dichVuCoGiay, type: 'CO_GIAY', label: 'cổ giày' },
    { field: 'idMucDichChay', service: dichVuMucDichChay, type: 'MUC_DICH_CHAY', label: 'mục đích sử dụng' }
];

const handleSave = async () => {
    if (!product.value.tenSanPham || !product.value.idThuongHieu || !product.value.idDanhMuc) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng điền đủ thông tin bắt buộc', color: 'error' });
        return;
    }

    let needsRefresh = false;
    const pendingUpdates = [];

    for (const config of attributeConfig) {
        const val = product.value[config.field];
        if (typeof val === 'string' && val.trim()) {
            try {
                const payload = { ten: val, ma: '', moTa: `Tự động tạo khi lưu sản phẩm` };
                const methodName = `tao${config.type.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')}`;
                const res = await config.service[methodName](payload);
                pendingUpdates.push({ field: config.field, id: res.id });
                needsRefresh = true;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: `Không thể tạo tự động ${config.label}`, color: 'error' });
                return;
            }
        }
    }

    if (needsRefresh) {
        await fetchFormOptions();

        for (const update of pendingUpdates) {
            product.value[update.field] = update.id;
        }
    }

    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'Xác nhận cập nhật' : 'Xác nhận thêm mới',
        message: isEditMode.value ? 'Bạn có chắc chắn muốn cập nhật thông tin sản phẩm này?' : 'Bạn có chắc chắn muốn thêm sản phẩm mới này?',
        color: 'success',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                const payload = {
                    ...product.value,
                    variants: []
                };

                if (isEditMode.value) {
                    await dichVuSanPham.capNhatSanPham(route.params.id, payload);
                    addNotification({ title: 'Thành công', subtitle: 'Cập nhật sản phẩm hoàn tất', color: 'success' });
                } else {
                    await dichVuSanPham.taoSanPham(payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã thêm sản phẩm mới', color: 'success' });
                }
                confirmDialog.value.show = false;
                router.push(PATH.SAN_PHAM);
            } catch (error) {
                addNotification({ title: 'Lỗi', subtitle: 'Không thể lưu sản phẩm', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in overflow-y-auto" style="height: 100vh;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
            { title: 'Danh sách sản phẩm', disabled: false, to: PATH.SAN_PHAM },
            { title: isEditMode ? 'Cập nhật' : 'Thêm mới', disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.push(PATH.SAN_PHAM)" class="btn-back-header">
                    <ArrowLeftIcon size="20" />
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn v-if="isEditMode" variant="outlined" color="primary"
                    class="text-none font-weight-bold px-6 rounded-lg h-11 border-2"
                    @click="router.push({ name: 'BienTheSanPham', query: { productId: route.params.id } })">
                    <BoxIcon size="18" class="mr-2" /> Quản lý biến thể
                </v-btn>
                <v-btn color="primary" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-lg h-11 elevation-4" :loading="saving"
                    @click="handleSave">
                    <DeviceFloppyIcon size="18" class="mr-2 text-white" /> Lưu thông tin
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
                <div class="mt-4 text-subtitle-1 font-weight-medium text-slate-500">Đang tải thông tin sản phẩm...</div>
            </v-col>
        </v-row>

        <v-row v-else class="form-grid pb-16">
            <!-- Left Column: Primary Content -->
            <v-col cols="12" lg="8">
                <!-- 1. General Identification -->
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <InfoCircleIcon size="18" class="text-primary" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin cơ bản</span>
                        </div>

                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="field-label">Mã sản phẩm</div>
                                <v-text-field v-model="product.maSanPham" readonly placeholder="SP-XXXX"
                                    variant="outlined" density="comfortable" class="custom-input mono-font bg-slate-50"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="8">
                                <div class="field-label">Tên sản phẩm <span class="text-red">*</span></div>
                                <v-text-field v-model="product.tenSanPham" placeholder="Ví dụ: Giày Nike Air..."
                                    :rules="[rules.required]" variant="outlined" density="comfortable"
                                    hide-details="auto"></v-text-field>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Mô tả ngắn</div>
                                <v-textarea v-model="product.moTaNgan" variant="outlined" rows="3"
                                    placeholder="Tóm tắt đặc điểm nổi bật..." hide-details
                                    class="custom-textarea"></v-textarea>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- 2. Detailed Attributes -->
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <SettingsIcon size="18" class="text-amber-darken-2" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thuộc tính kỹ thuật</span>
                        </div>

                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="field-label">Xuất xứ <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idXuatXu" :items="origins" item-title="ten" item-value="id"
                                    :rules="[rules.required]" placeholder="Chọn xuất xứ..." variant="outlined"
                                    density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idXuatXu', dichVuXuatXu, 'XUAT_XU', 'xuất xứ')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Chất liệu <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idChatLieu" :items="materials" item-title="ten"
                                    item-value="id" :rules="[rules.required]" placeholder="Chọn chất liệu..."
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idChatLieu', dichVuChatLieu, 'CHAT_LIEU', 'chất liệu')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Loại đế <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idDeGiay" :items="soles" item-title="ten" item-value="id"
                                    :rules="[rules.required]" variant="outlined" density="comfortable"
                                    :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idDeGiay', dichVuDeGiay, 'DE_GIAY', 'đế giày')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Loại cổ <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idCoGiay" :items="collars" item-title="ten" item-value="id"
                                    :rules="[rules.required]" variant="outlined" density="comfortable"
                                    :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idCoGiay', dichVuCoGiay, 'CO_GIAY', 'cổ giày')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Mục đích sử dụng <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idMucDichChay" :items="purposes" item-title="ten"
                                    item-value="id" :rules="[rules.required]" variant="outlined" density="comfortable"
                                    :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idMucDichChay', dichVuMucDichChay, 'MUC_DICH_CHAY', 'mục đích sử dụng')"></v-combobox>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>
            </v-col>

            <!-- Right Column -->
            <v-col cols="12" lg="4">
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-emerald-lighten-5 mr-3">
                                <BoxIcon size="18" class="text-emerald-darken-2" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Phân loại</span>
                        </div>

                        <v-row>
                            <v-col cols="12">
                                <div class="field-label">Thương hiệu <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idThuongHieu" :items="brands" item-title="ten"
                                    item-value="id" :rules="[rules.required]" placeholder="Tìm thương hiệu..."
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idThuongHieu', dichVuThuongHieu, 'THUONG_HIEU', 'thương hiệu')"></v-combobox>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Danh mục <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idDanhMuc" :items="categories" item-title="ten"
                                    item-value="id" :rules="[rules.required]" placeholder="Chọn danh mục..."
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idDanhMuc', dichVuDanhMuc, 'DANH_MUC', 'danh mục')"></v-combobox>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Đối tượng sử dụng <span class="text-red">*</span></div>
                                <v-btn-toggle v-model="product.gioiTinhKhachHang" mandatory color="primary"
                                    variant="outlined" density="comfortable" class="w-100 rounded-lg custom-toggle">
                                    <v-btn value="NAM" class="flex-grow-1">Nam</v-btn>
                                    <v-btn value="NU" class="flex-grow-1">Nữ</v-btn>
                                    <v-btn value="UNISEX" class="flex-grow-1">Unisex</v-btn>
                                </v-btn-toggle>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

            </v-col>
        </v-row>

        <!-- Confirmation Dialog -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
/* Redundant local styles removed - now using global _admin-common.scss */
@media (min-width: 1280px) {
    .sticky-sidebar {
        position: sticky;
        top: 0;
    }
}
</style>
