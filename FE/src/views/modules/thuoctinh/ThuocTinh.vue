<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { PATH } from '@/router/routePaths';
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
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';

// REUSABLE COMPONENTS
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import AttributeFilter from './components/AttributeFilter.vue';
import AttributeTable from './components/AttributeTable.vue';
import AttributeFormModal from './components/AttributeFormModal.vue';

import { useConfirmDialog } from '@/composables/useConfirmDialog';

const { addNotification } = useNotifications();

const loading = ref(false);
const isRefreshing = ref(false);
const selectedTab = ref('brands');
const searchQuery = ref('');
const statusFilter = ref(null);

const pagination = ref({
    page: 1,
    size: 5,
    totalElements: 0,
    totalPages: 1
});

const showDialog = ref(false);
const selectedItem = ref(null);
const isEditMode = ref(false);

const route = useRoute();
const router = useRouter();

// Use composables
const { confirmDialog, setConfirm, handleConfirm } = useConfirmDialog();

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

const currentMeta = computed(() => attributeMeta[selectedTab.value] || attributeMeta.brands);

const tableHeaders = computed(() => [
    { text: 'STT', width: '50px' },
    { text: currentMeta.value.codeLabel, width: '120px' },
    { text: currentMeta.value.nameLabel, width: '220px' },
    { text: 'Trạng thái', width: '130px' },
    { text: 'Thao tác', width: '120px' }
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
            case 'brands': response = await service.layThuongHieu(params); break;
            case 'categories': response = await service.layDanhMuc(params); break;
            case 'colors': response = await service.layMauSac(params); break;
            case 'sizes': response = await service.layKichThuoc(params); break;
            case 'materials': response = await service.layChatLieu(params); break;
            case 'soles': response = await service.layDeGiay(params); break;
            case 'collars': response = await service.layCoGiay(params); break;
            case 'origins': response = await service.layXuatXu(params); break;
            case 'purposes': response = await service.layMucDichChay(params); break;
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
    setConfirm({
        title: `Xác nhận ${modeText.toLowerCase()}`,
        message: `Bạn có chắc muốn lưu [${itemForm.value.ten}] vào danh sách ${getCurrentTabTitle()}?`,
        color: 'success',
        action: async () => {
            if (isEditMode.value) await updateItem();
            else await createItem();
        }
    });
};

const createItem = async () => {
    const service = services[selectedTab.value];
    let res;
    switch (selectedTab.value) {
        case 'brands': res = await service.taoThuongHieu(itemForm.value); break;
        case 'categories': res = await service.taoDanhMuc(itemForm.value); break;
        case 'colors': res = await service.taoMauSac(itemForm.value); break;
        case 'sizes': res = await service.taoKichThuoc(itemForm.value); break;
        case 'materials': res = await service.taoChatLieu(itemForm.value); break;
        case 'soles': res = await service.taoDeGiay(itemForm.value); break;
        case 'collars': res = await service.taoCoGiay(itemForm.value); break;
        case 'origins': res = await service.taoXuatXu(itemForm.value); break;
        case 'purposes': res = await service.taoMucDichChay(itemForm.value); break;
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
        case 'brands': res = await service.capNhatThuongHieu(id, itemForm.value); break;
        case 'categories': res = await service.capNhatDanhMuc(id, itemForm.value); break;
        case 'colors': res = await service.capNhatMauSac(id, itemForm.value); break;
        case 'sizes': res = await service.capNhatKichThuoc(id, itemForm.value); break;
        case 'materials': res = await service.capNhatChatLieu(id, itemForm.value); break;
        case 'soles': res = await service.capNhatDeGiay(id, itemForm.value); break;
        case 'collars': res = await service.capNhatCoGiay(id, itemForm.value); break;
        case 'origins': res = await service.capNhatXuatXu(id, itemForm.value); break;
        case 'purposes': res = await service.capNhatMucDichChay(id, itemForm.value); break;
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
    setConfirm({
        title: 'Xác nhận trạng thái',
        message: `Bạn có muốn đổi trạng thái của [${getItemName(item)}]?`,
        color: 'warning',
        action: async () => {
            await changeItemStatus(item);
        }
    });
};

const changeItemStatus = async (item) => {
    const service = services[selectedTab.value];
    const newS = isActiveStatus(item.trangThai) ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
    const pld = { ...item, trangThai: newS };
    switch (selectedTab.value) {
        case 'brands': await service.capNhatThuongHieu(item.id, pld); break;
        case 'categories': await service.capNhatDanhMuc(item.id, pld); break;
        case 'colors': await service.capNhatMauSac(item.id, pld); break;
        case 'sizes': await service.capNhatKichThuoc(item.id, pld); break;
        case 'materials': await service.capNhatChatLieu(item.id, pld); break;
        case 'soles': await service.capNhatDeGiay(item.id, pld); break;
        case 'collars': await service.capNhatCoGiay(item.id, pld); break;
        case 'origins': await service.capNhatXuatXu(item.id, pld); break;
        case 'purposes': await service.capNhatMucDichChay(item.id, pld); break;
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
    router.replace(`${PATH.THUOC_TINH}/${reverseRouteMap[n]}`);
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in d-flex flex-column overflow-hidden font-body"
        style="height: calc(100vh - 20px);">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
                { title: getCurrentTabTitle(), disabled: true }
            ]"
        />

        <div class="mb-2"></div>

        <!-- 1. FILTER -->
        <AttributeFilter v-model:searchQuery="searchQuery" v-model:statusFilter="statusFilter" :loading="loading"
            :isRefreshing="isRefreshing" @refresh="handleRefresh" @search="loadItems"
            @update:statusFilter="loadItems" />

        <!-- 2. TABLE -->
        <AttributeTable 
            v-model:tab="selectedTab" 
            :title="`Danh sách ${getCurrentTabTitle()}`" 
            :headers="tableHeaders"
            :items="dataRefs[selectedTab].value" 
            :loading="loading" 
            :pagination="pagination" 
            :tabs="tabs"
            :meta="currentMeta" 
            @add="openCreateDialog" 
            @edit="editItem" 
            @change-status="confirmChangeStatus"
            @load-items="loadItems" 
            @update:size="pagination.size = $event" 
            class="flex-grow-1 min-h-0"
        />

        <!-- FORM MODAL -->
        <AttributeFormModal v-model:show="showDialog" v-model:form="itemForm" :isEditMode="isEditMode"
            :title="getCurrentTabTitle()" :selectedTab="selectedTab" @save="confirmSaveItem" />

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)" />
    </v-container>
</template>

<style scoped>
.font-body {
    font-family: 'Inter', sans-serif;
}

:deep(.data-cell), :deep(.data-cell *) {
    font-size: 14px !important;
}
</style>

