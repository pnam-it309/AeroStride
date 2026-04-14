<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
    dichVuThuongHieu,
    dichVuDanhMuc,
    dichVuMauSac,
    dichVuKichThuoc,
    dichVuChatLieu,
    dichVuDeGiay,
    dichVuCoGiay,
    dichVuXuatXu,
    dichVuMucDichChay
} from '@/services/product/dichVuThuocTinh';
import { useNotifications } from '@/services/notificationService';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { EditIcon } from 'vue-tabler-icons';

const { addNotification } = useNotifications();

const loading = ref(false);
const isRefreshing = ref(false);
const selectedTab = ref('brands');
const searchQuery = ref('');
const statusFilter = ref(null);

const pagination = ref({
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 1
});

const showDialog = ref(false);
const selectedItem = ref(null);
const isEditMode = ref(false);

const route = useRoute();
const router = useRouter();

// Confirmation Logic
const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

const routeMap = {
    'thuong-hieu': 'brands',
    'danh-muc': 'categories',
    'xuat-xu': 'origins',
    'muc-dich-chay': 'purposes',
    'chat-lieu': 'materials',
    'de-giay': 'soles',
    'co-giay': 'collars',
    'mau-sac': 'colors',
    'kich-thuoc': 'sizes'
};

const reverseRouteMap = Object.entries(routeMap).reduce((acc, [k, v]) => {
    acc[v] = k;
    return acc;
}, {});

const brands = ref([]);
const categories = ref([]);
const colors = ref([]);
const sizes = ref([]);
const materials = ref([]);
const soles = ref([]);
const collars = ref([]);
const origins = ref([]);
const purposes = ref([]);

const itemForm = ref({ ten: '', moTa: '', trangThai: 'DANG_HOAT_DONG', maMauHex: '#000000', giaTriKichThuoc: '' });

const tabs = [
    { value: 'brands', title: 'Thương hiệu', icon: 'mdi-tag' },
    { value: 'categories', title: 'Danh mục', icon: 'mdi-folder' },
    { value: 'colors', title: 'Màu sắc', icon: 'mdi-palette' },
    { value: 'sizes', title: 'Kích thước', icon: 'mdi-ruler' },
    { value: 'materials', title: 'Chất liệu', icon: 'mdi-texture' },
    { value: 'soles', title: 'Đế giày', icon: 'mdi-shoe-sole' },
    { value: 'collars', title: 'Cổ giày', icon: 'mdi-shoe-formal' },
    { value: 'origins', title: 'Xuất xứ', icon: 'mdi-map-marker' },
    { value: 'purposes', title: 'Mục đích', icon: 'mdi-run' }
];

const services = {
    brands: dichVuThuongHieu,
    categories: dichVuDanhMuc,
    colors: dichVuMauSac,
    sizes: dichVuKichThuoc,
    materials: dichVuChatLieu,
    soles: dichVuDeGiay,
    collars: dichVuCoGiay,
    origins: dichVuXuatXu,
    purposes: dichVuMucDichChay
};

const dataRefs = { brands, categories, colors, sizes, materials, soles, collars, origins, purposes };

const attributeMeta = {
    brands: {
        codeLabel: 'Mã thương hiệu',
        nameLabel: 'Tên thương hiệu',
        codeKeys: ['maThuongHieu', 'ma', 'ma_thuong_hieu'],
        nameKeys: ['tenThuongHieu', 'ten', 'ten_thuong_hieu']
    },
    categories: {
        codeLabel: 'Mã danh mục',
        nameLabel: 'Tên danh mục',
        codeKeys: ['maDanhMuc', 'ma', 'ma_danh_muc'],
        nameKeys: ['tenDanhMuc', 'ten', 'ten_danh_muc']
    },
    colors: {
        codeLabel: 'Mã màu sắc',
        nameLabel: 'Tên màu sắc',
        codeKeys: ['maMauSac', 'ma', 'ma_mau_sac'],
        nameKeys: ['tenMauSac', 'ten', 'ten_mau_sac']
    },
    sizes: {
        codeLabel: 'Mã kích thước',
        nameLabel: 'Tên kích thước',
        codeKeys: ['maKichThuoc', 'ma', 'ma_kich_thuoc'],
        nameKeys: ['tenKichThuoc', 'ten', 'ten_kich_thuoc', 'giaTriKichThuoc', 'gia_tri_kich_thuoc']
    },
    materials: {
        codeLabel: 'Mã chất liệu',
        nameLabel: 'Tên chất liệu',
        codeKeys: ['maChatLieu', 'ma', 'ma_chat_lieu'],
        nameKeys: ['tenChatLieu', 'ten', 'ten_chat_lieu']
    },
    soles: {
        codeLabel: 'Mã đế giày',
        nameLabel: 'Tên đế giày',
        codeKeys: ['maDeGiay', 'ma', 'ma_de_giay'],
        nameKeys: ['tenDeGiay', 'ten', 'ten_de_giay']
    },
    collars: {
        codeLabel: 'Mã cổ giày',
        nameLabel: 'Tên cổ giày',
        codeKeys: ['maCoGiay', 'ma', 'ma_co_giay'],
        nameKeys: ['tenCoGiay', 'ten', 'ten_co_giay']
    },
    origins: {
        codeLabel: 'Mã xuất xứ',
        nameLabel: 'Nơi xuất xứ',
        codeKeys: ['maXuatXu', 'ma', 'ma_xuat_xu'],
        nameKeys: ['tenXuatXu', 'noiXuatXu', 'ten', 'ten_xuat_xu', 'noi_xuat_xu']
    },
    purposes: {
        codeLabel: 'Mã mục đích chạy',
        nameLabel: 'Tên mục đích chạy',
        codeKeys: ['maMucDichChay', 'ma', 'ma_muc_dich_chay'],
        nameKeys: ['tenMucDichChay', 'ten', 'ten_muc_dich_chay']
    }
};

const filteredItems = computed(() => {
    const list = dataRefs[selectedTab.value]?.value || [];
    if (!searchQuery.value) return list;
    const q = searchQuery.value.toLowerCase().trim();
    return list.filter((item) => {
        const name = getItemName(item).toLowerCase();
        const code = getItemCode(item).toLowerCase();
        return name.includes(q) || code.includes(q);
    });
});

const currentMeta = computed(() => attributeMeta[selectedTab.value] || attributeMeta.brands);

const tableHeaders = computed(() => [
    { text: 'STT', align: 'center', width: '8%' },
    { text: currentMeta.value.codeLabel, align: 'left', width: '18%' },
    { text: currentMeta.value.nameLabel, align: 'left', width: '20%' },
    { text: 'Trạng thái', align: 'center', width: '15%' },
    { text: 'Ngày tạo', align: 'center', width: '17%' },
    { text: 'Hành động', align: 'center', width: '10%' }
]);

const pickFirst = (item, keys, fallback = '--') => {
    for (const key of keys) {
        const val = item?.[key];
        if (val !== null && val !== undefined && String(val).trim() !== '') return val;
    }
    return fallback;
};

const getItemCode = (item) => String(pickFirst(item, currentMeta.value.codeKeys));

const getItemName = (item) => String(pickFirst(item, currentMeta.value.nameKeys));

const isActiveStatus = (status) => {
    if (status === null || status === undefined) return false;
    if (typeof status === 'number') return status === 0;
    const normalized = String(status).toUpperCase();
    return normalized === 'DANG_HOAT_DONG' || normalized === 'ACTIVE' || normalized === 'HOAT_DONG' || normalized === '0';
};

const getDisplayStatus = (status) => (isActiveStatus(status) ? 'Hoạt động' : 'Ngừng hoạt động');

const getStatusChipClass = (status) => (isActiveStatus(status) ? 'attr-status-active' : 'attr-status-inactive');

const getCreatedAt = (item) => item?.ngayTao ?? item?.createdAt ?? item?.ngay_tao ?? null;

const formatDateTime = (value) => {
    if (!value) return '--';
    const date = typeof value === 'number' ? new Date(value) : new Date(String(value));
    if (Number.isNaN(date.getTime())) return '--';
    const d = String(date.getDate()).padStart(2, '0');
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const y = date.getFullYear();
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    return `${d}/${m}/${y} ${hh}:${mm}`;
};

const loadItems = async () => {
    loading.value = true;
    try {
        const service = services[selectedTab.value];
        let response;
        const params = {
            page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
            size: pagination.value.size,
            keyword: searchQuery.value || null,
            trangThai: statusFilter.value !== null ? statusFilter.value : null
        };

        switch (selectedTab.value) {
            case 'brands':
                response = await service.layThuongHieu(params);
                break;
            case 'categories':
                response = await service.layDanhMuc(params);
                break;
            case 'colors':
                response = await service.layMauSac(params);
                break;
            case 'sizes':
                response = await service.layKichThuoc(params);
                break;
            case 'materials':
                response = await service.layChatLieu(params);
                break;
            case 'soles':
                response = await service.layDeGiay(params);
                break;
            case 'collars':
                response = await service.layCoGiay(params);
                break;
            case 'origins':
                response = await service.layXuatXu(params);
                break;
            case 'purposes':
                response = await service.layMucDichChay(params);
                break;
        }

        if (response) {
            dataRefs[selectedTab.value].value = response.content || [];
            pagination.value.totalPages = response.totalPages || 1;
            pagination.value.totalElements = response.totalElements || 0;
        }
    } catch (error) {
        console.error(error);
    } finally {
        loading.value = false;
    }
};

const confirmSaveItem = () => {
    const modeText = isEditMode.value ? 'CẬP NHẬT' : 'THÊM MỚI';
    confirmDialog.value = {
        show: true,
        title: `Xác nhận ${modeText.toLowerCase()}`,
        message: `Bạn có chắc muốn lưu [${itemForm.value.ten}] vào danh sách ${getCurrentTabTitle()}?`,
        color: 'success',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                if (isEditMode.value) await updateItem();
                else await createItem();
                confirmDialog.value.show = false;
            } catch (error) {
                console.error(error);
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const createItem = async () => {
    const service = services[selectedTab.value];
    let res;
    switch (selectedTab.value) {
        case 'brands':
            res = await service.taoThuongHieu(itemForm.value);
            break;
        case 'categories':
            res = await service.taoDanhMuc(itemForm.value);
            break;
        case 'colors':
            res = await service.taoMauSac(itemForm.value);
            break;
        case 'sizes':
            res = await service.taoKichThuoc(itemForm.value);
            break;
        case 'materials':
            res = await service.taoChatLieu(itemForm.value);
            break;
        case 'soles':
            res = await service.taoDeGiay(itemForm.value);
            break;
        case 'collars':
            res = await service.taoCoGiay(itemForm.value);
            break;
        case 'origins':
            res = await service.taoXuatXu(itemForm.value);
            break;
        case 'purposes':
            res = await service.taoMucDichChay(itemForm.value);
            break;
    }
    addNotification({
        title: 'Thêm mới thành công',
        subtitle: `Đã thêm [${itemForm.value.ten}] vào danh mục ${getCurrentTabTitle()}`,
        icon: 'CircleCheckIcon',
        color: 'success'
    });
    dataRefs[selectedTab.value].value.unshift(res);
    showDialog.value = false;
    loadItems();
};

const updateItem = async () => {
    const service = services[selectedTab.value];
    let res;
    const id = selectedItem.value.id;
    switch (selectedTab.value) {
        case 'brands':
            res = await service.capNhatThuongHieu(id, itemForm.value);
            break;
        case 'categories':
            res = await service.capNhatDanhMuc(id, itemForm.value);
            break;
        case 'colors':
            res = await service.capNhatMauSac(id, itemForm.value);
            break;
        case 'sizes':
            res = await service.capNhatKichThuoc(id, itemForm.value);
            break;
        case 'materials':
            res = await service.capNhatChatLieu(id, itemForm.value);
            break;
        case 'soles':
            res = await service.capNhatDeGiay(id, itemForm.value);
            break;
        case 'collars':
            res = await service.capNhatCoGiay(id, itemForm.value);
            break;
        case 'origins':
            res = await service.capNhatXuatXu(id, itemForm.value);
            break;
        case 'purposes':
            res = await service.capNhatMucDichChay(id, itemForm.value);
            break;
    }
    const idx = dataRefs[selectedTab.value].value.findIndex((i) => i.id === id);
    if (idx !== -1) dataRefs[selectedTab.value].value[idx] = res;
    addNotification({
        title: 'Cập nhật thành công',
        subtitle: `Đã cập nhật thông tin cho [${itemForm.value.ten}]`,
        icon: 'EditIcon',
        color: 'primary'
    });
    showDialog.value = false;
};

const confirmChangeStatus = (item) => {
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận trạng thái',
        message: `Bạn có muốn đổi trạng thái của [${getItemName(item)}]?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await changeItemStatus(item);
                confirmDialog.value.show = false;
            } catch (e) {
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const changeItemStatus = async (item) => {
    const service = services[selectedTab.value];
    const newS = isActiveStatus(item.trangThai) ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
    const pld = { ...item, trangThai: newS };
    switch (selectedTab.value) {
        case 'brands':
            await service.capNhatThuongHieu(item.id, pld);
            break;
        case 'categories':
            await service.capNhatDanhMuc(item.id, pld);
            break;
        case 'colors':
            await service.capNhatMauSac(item.id, pld);
            break;
        case 'sizes':
            await service.capNhatKichThuoc(item.id, pld);
            break;
        case 'materials':
            await service.capNhatChatLieu(item.id, pld);
            break;
        case 'soles':
            await service.capNhatDeGiay(item.id, pld);
            break;
        case 'collars':
            await service.capNhatCoGiay(item.id, pld);
            break;
        case 'origins':
            await service.capNhatXuatXu(item.id, pld);
            break;
        case 'purposes':
            await service.capNhatMucDichChay(item.id, pld);
            break;
    }
    item.trangThai = newS;
    addNotification({
        title: 'Đổi trạng thái',
        subtitle: `Thuộc tính [${getItemName(item)}] đã chuyển trạng thái.`,
        icon: 'InfoCircleIcon',
        color: 'warning'
    });
};

const resetForm = () => {
    itemForm.value = { ten: '', moTa: '', trangThai: 'DANG_HOAT_DONG', maMauHex: '#000000', giaTriKichThuoc: '' };
    selectedItem.value = null;
};
const openCreateDialog = () => {
    resetForm();
    isEditMode.value = false;
    showDialog.value = true;
};
const handleRefresh = async () => {
    isRefreshing.value = true;
    searchQuery.value = '';
    statusFilter.value = null;
    pagination.value.page = 1;
    await loadItems();
    setTimeout(() => {
        isRefreshing.value = false;
    }, 800);
};
const editItem = (item) => {
    selectedItem.value = item;
    itemForm.value = { ...item };
    isEditMode.value = true;
    showDialog.value = true;
};
const getCurrentTabTitle = () => tabs.find((t) => t.value === selectedTab.value)?.title || '';

onMounted(() => {
    if (!route.params.tab) loadItems();
});
watch(
    () => route.params.tab,
    (n) => {
        if (n && routeMap[n]) {
            selectedTab.value = routeMap[n];
            loadItems();
        }
    },
    { immediate: true }
);
watch(selectedTab, (n) => {
    router.replace(`/thuoc-tinh/${reverseRouteMap[n]}`);
});
</script>

<template>
    <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
        <!-- Header -->
        <div class="d-flex justify-space-between align-center mb-4">
            <div>
                <h1 class="text-h4 font-weight-bold">Quản lí {{ getCurrentTabTitle() }}</h1>
            </div>
        </div>

        <!-- 1. FILTER -->
        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc thuộc tính" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm nhanh</div>
                    <v-text-field
                        v-model="searchQuery"
                        placeholder="Nhập tên hoặc mã thuộc tính..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input font-weight-bold"
                        @keyup.enter="loadItems"
                    ></v-text-field>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select
                        v-model="statusFilter"
                        :items="[
                            { title: 'Tất cả trạng thái', value: null },
                            { title: 'Hoạt động', value: 'DANG_HOAT_DONG' },
                            { title: 'Không hoạt động', value: 'KHONG_HOAT_DONG' }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input font-weight-bold"
                        @update:model-value="loadItems"
                    ></v-select>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable
            :title="`Danh sách ${getCurrentTabTitle().toLowerCase()}`"
            :addButtonText="`Thêm ${getCurrentTabTitle().toLowerCase()}`"
            :headers="tableHeaders"
            :items="dataRefs[selectedTab].value"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="openCreateDialog"
        >
            <template #top>
                <v-tabs v-model="selectedTab" color="primary" @update:model-value="loadItems" class="attribute-tabs" grow>
                    <v-tab v-for="tab in tabs" :key="tab.value" :value="tab.value" class="text-none font-weight-medium attr-tab-item">
                        <v-icon start size="16">{{ tab.icon }}</v-icon>
                        {{ tab.title }}
                    </v-tab>
                </v-tabs>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell stt-cell">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell text-left font-weight-medium text-dark">{{ getItemCode(item) }}</td>
                    <td class="data-cell text-left text-medium-emphasis">{{ getItemName(item) }}</td>
                    <td class="data-cell" style="text-align: center">
                        <v-chip
                            variant="flat"
                            class="font-weight-medium px-3 attr-status-chip"
                            :class="getStatusChipClass(item.trangThai)"
                            >{{ getDisplayStatus(item.trangThai) }}</v-chip
                        >
                    </td>
                    <td class="data-cell" style="text-align: center">{{ formatDateTime(getCreatedAt(item)) }}</td>
                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-controls">
                            <div class="switch-wrapper">
                                <v-switch
                                    :model-value="isActiveStatus(item.trangThai)"
                                    color="#1e3a8a"
                                    hide-details
                                    density="compact"
                                    class="tight-switch action-switch"
                                    @click.prevent.stop="confirmChangeStatus(item)"
                                />
                                <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                            </div>
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#5f6f82"
                                class="rounded-lg action-icon-btn"
                                @click.stop="editItem(item)"
                            >
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event"
                    @update:page-size="pagination.size = $event"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="dataRefs[selectedTab].value.length"
                    @change="loadItems"
                />
            </template>
        </AdminTable>

        <!-- DIALOG (SQUARE) -->
        <v-dialog v-model="showDialog" max-width="500">
            <v-card class="rounded-lg border shadow-2xl">
                <v-card-title class="pa-4 font-weight-medium border-b bg-grey-lighten-4 text-#2E4E8E"
                    >{{ isEditMode ? 'Cập nhật' : 'Thêm mới' }} {{ getCurrentTabTitle().toLowerCase() }}</v-card-title
                >
                <v-card-text class="pa-6">
                    <v-form>
                        <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">
                            Mã {{ getCurrentTabTitle() }} <span class="text-caption text-#2E4E8E">(Hệ thống tự tạo)</span>
                        </div>
                        <v-text-field
                            v-model="itemForm.ma"
                            readonly
                            placeholder="Hệ thống tự tạo..."
                            variant="outlined"
                            class="mb-4 font-weight-black"
                            rounded="0"
                            density="comfortable"
                            hide-details
                        ></v-text-field>

                        <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên {{ getCurrentTabTitle() }} *</div>
                        <v-text-field
                            v-model="itemForm.ten"
                            placeholder="Nhập tên..."
                            variant="outlined"
                            class="mb-4 font-weight-medium"
                            rounded="0"
                            density="comfortable"
                            hide-details
                        ></v-text-field>

                        <!-- Color Specific -->
                        <div v-if="selectedTab === 'colors'" class="mb-4">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mã màu (Hex)</div>
                            <div class="d-flex align-center gap-4">
                                <v-text-field
                                    v-model="itemForm.maMauHex"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    rounded="0"
                                ></v-text-field>
                                <input
                                    type="color"
                                    v-model="itemForm.maMauHex"
                                    style="width: 48px; height: 48px; border: 1px solid #ddd; padding: 2px"
                                />
                            </div>
                        </div>

                        <!-- Size Specific -->
                        <div v-if="selectedTab === 'sizes'" class="mb-4">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Giá trị kích thước (Số)</div>
                            <v-text-field
                                v-model="itemForm.giaTriKichThuoc"
                                type="number"
                                placeholder="Ví dụ: 42"
                                variant="outlined"
                                density="comfortable"
                                hide-details
                                rounded="0"
                            ></v-text-field>
                        </div>

                        <v-textarea
                            v-model="itemForm.moTa"
                            label="Mô tả"
                            variant="outlined"
                            rows="3"
                            class="mb-4 font-weight-medium"
                            rounded="0"
                        ></v-textarea>
                    </v-form>
                </v-card-text>
                <v-divider></v-divider>
                <v-card-actions class="pa-4 bg-grey-lighten-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" class="text-none font-weight-medium" @click="showDialog = false">Hủy bỏ</v-btn>
                    <v-btn
                        color="#2E4E8E"
                        variant="flat"
                        rounded="0"
                        class="px-8 text-none font-weight-medium"
                        @click="confirmSaveItem"
                        :disabled="!itemForm.ten"
                        >Xác nhận</v-btn
                    >
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- SHARED CONFIRM -->
        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="confirmDialog.action"
        />
    </v-container>
</template>

<style scoped>
.gray-bg {
    background-color: #f5f7fb;
}

.text-dark {
    color: #0f172a !important;
}

.tight-switch {
    transform: none;
    width: 36px;
}

.font-body {
    font-family: 'Inter', sans-serif;
}

.filter-top {
    position: sticky;
    top: 8px;
    z-index: 6;
}

.invoice-filter-shell :deep(.filter-card) {
    border-radius: 14px !important;
    margin-bottom: 8px !important;
}

.invoice-filter-shell :deep(.v-card-text) {
    padding-top: 12px !important;
    padding-bottom: 12px !important;
}

.invoice-filter-shell :deep(.filter-header) {
    margin-bottom: 12px !important;
}

.filter-top :deep(.v-card-text) {
    padding-top: 10px !important;
    padding-bottom: 10px !important;
}

.filter-top :deep(.filter-header) {
    margin-bottom: 8px !important;
}

.filter-top :deep(.v-field) {
    min-height: 40px;
}

.filter-top :deep(.v-input__control) {
    min-height: 40px;
}

.filter-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #0f172a;
    margin-bottom: 4px;
}

.filter-top :deep(.search-field input::placeholder) {
    font-size: 13px !important;
}

.filter-top :deep(.filter-reset-col) {
    margin-left: auto !important;
    flex: 0 0 8.333333% !important;
    max-width: 8.333333% !important;
    width: 8.333333% !important;
    align-self: flex-end !important;
    justify-content: flex-end !important;
}

:deep(.native-admin-table .header-cell:nth-child(2)),
:deep(.native-admin-table .header-cell:nth-child(3)),
:deep(.native-admin-table .data-cell:nth-child(2)),
:deep(.native-admin-table .data-cell:nth-child(3)) {
    text-align: left !important;
}

:deep(.native-admin-table .header-cell:nth-child(1)),
:deep(.native-admin-table .data-cell.stt-cell) {
    text-align: center !important;
    padding-left: 0 !important;
}

:deep(.native-admin-table .data-cell:nth-child(1)),
:deep(.native-admin-table .data-cell:nth-child(2)) {
    color: #0f172a !important;
    font-weight: 700 !important;
}

:deep(.native-admin-table .header-cell) {
    font-size: 13px !important;
    font-weight: 700 !important;
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .data-cell) {
    font-size: 13px !important;
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .data-cell:not(.action-cell)) {
    font-weight: 600 !important;
}

:deep(.native-admin-table .data-cell .text-subtitle-2),
:deep(.native-admin-table .data-cell .text-caption),
:deep(.native-admin-table .data-cell .text-body-2),
:deep(.native-admin-table .data-cell .text-subtitle-1) {
    font-size: 13px !important;
    font-weight: 600 !important;
    line-height: 1.35 !important;
}

:deep(.attribute-tabs) {
    border-bottom: 1px solid #e2e8f0;
}

:deep(.attribute-tabs .v-slide-group__content) {
    width: 100%;
    gap: 0;
    padding: 0;
}

:deep(.attribute-tabs .attr-tab-item) {
    min-height: 46px !important;
    color: #334155 !important;
    font-size: 14px !important;
    border-bottom: 2px solid transparent !important;
    padding-inline: 8px !important;
    flex: 1 1 0 !important;
    max-width: none !important;
}

:deep(.attribute-tabs .v-tab--selected) {
    color: #1e3a8a !important;
    border-bottom-color: #1e3a8a !important;
}

.action-controls {
    gap: 6px;
}

:deep(.action-cell .action-icon-btn) {
    background: transparent !important;
    border: none !important;
    outline: none !important;
    box-shadow: none !important;
    min-width: 28px !important;
    width: 28px !important;
    height: 28px !important;
    padding: 0 !important;
}

:deep(.action-cell .action-icon-btn .v-btn__overlay),
:deep(.action-cell .action-icon-btn .v-btn__underlay),
:deep(.action-cell .action-icon-btn .v-ripple__container) {
    display: none !important;
}

:deep(.action-cell .action-switch .v-selection-control__wrapper) {
    width: 36px !important;
    min-width: 36px !important;
    height: 22px !important;
}

.switch-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

:deep(.action-cell .action-switch .v-switch__track) {
    background: #ffffff !important;
    border: 1px solid #cbd5e1 !important;
    opacity: 1 !important;
    min-height: 18px !important;
    max-height: 18px !important;
    width: 32px !important;
}

:deep(.action-cell .action-switch .v-selection-control--dirty .v-switch__track) {
    background: #d9e6fb !important;
    border-color: #d9e6fb !important;
}

:deep(.action-cell .action-switch .v-switch__thumb) {
    background: #94a3b8 !important;
    width: 14px !important;
    height: 14px !important;
    box-shadow: none !important;
}

:deep(.action-cell .action-switch .v-selection-control--dirty .v-switch__thumb) {
    background: #1e3a8a !important;
}

:deep(.attr-status-chip) {
    font-size: 12px !important;
    min-height: 24px !important;
    border-radius: 999px !important;
    border: 1px solid transparent !important;
    font-weight: 700 !important;
}

:deep(.attr-status-chip.attr-status-active) {
    background-color: #e8f7f3 !important;
    color: #2f8f86 !important;
    border-color: #c9ece5 !important;
}

:deep(.attr-status-chip.attr-status-inactive) {
    background-color: #fff3e6 !important;
    color: #a87535 !important;
    border-color: #f3dfc6 !important;
}
</style>
