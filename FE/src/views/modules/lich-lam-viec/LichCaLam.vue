<script setup>
import { ref, onMounted, computed } from 'vue';
import { AdminFilter, AdminTable, AdminPagination, AdminBreadcrumbs } from '@/components/common';
import apiService from '@/services/apiService';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';
import { ADMIN_ICONS } from '@/constants/adminIcons';
import { useNotifications } from '@/services/notificationService';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import AdminConfirm from '@/components/common/AdminConfirm.vue';

const { addNotification } = useNotifications();
const { confirmDialog, setConfirm, handleConfirm } = useConfirmDialog();

const loading = ref(false);
const isRefreshing = ref(false);
const items = ref([]);

const pagination = ref({
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 0
});

const filters = ref({
    search: ''
});

// Dialog state
const showDialog = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const form = ref({
    tenCa: '',
    gioBatDau: '08:00',
    gioKetThuc: '17:00',
    moTa: ''
});

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch ca làm', disabled: true }
];

const tableHeaders = [
    { text: 'STT', width: '50px' },
    { text: 'Thông tin ca làm', width: '200px' },
    { text: 'Giờ bắt đầu', width: '150px' },
    { text: 'Giờ kết thúc', width: '150px' },
    { text: 'Mô tả', width: '150px' },
    { text: 'Hành động', width: '100px' }
];

const loadData = async (showLoading = true) => {
    if (showLoading) loading.value = true;
    try {
        const response = await apiService.get(API_LICH_LAM_VIEC.SHIFTS);
        if (response.data.success) {
            items.value = response.data.data;
            pagination.value.totalElements = filteredItems.value.length;
            pagination.value.totalPages = Math.ceil(filteredItems.value.length / pagination.value.size);
        }
    } catch (error) {
        console.error('Error fetching shifts:', error);
    } finally {
        if (showLoading) loading.value = false;
    }
};

const filteredItems = computed(() => {
    return items.value.filter(item => {
        return !filters.value.search || 
            item.tenCa.toLowerCase().includes(filters.value.search.toLowerCase()) ||
            (item.moTa && item.moTa.toLowerCase().includes(filters.value.search.toLowerCase()));
    });
});

const getShiftChipClass = (caName) => {
    if (!caName) return 'shift-chip-default';
    const name = caName.toLowerCase();
    if (name.includes('sáng') || name.includes('sang')) return 'shift-chip-morning';
    if (name.includes('chiều') || name.includes('chieu')) return 'shift-chip-afternoon';
    if (name.includes('tối') || name.includes('toi') || name.includes('đêm')) return 'shift-chip-night';
    return 'shift-chip-default';
};

const paginatedItems = computed(() => {
    const start = (pagination.value.page - 1) * pagination.value.size;
    return filteredItems.value.slice(start, start + pagination.value.size);
});

const handleRefresh = async () => {
    isRefreshing.value = true;
    await loadData();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleFilter = () => {
    pagination.value.page = 1;
};

// Shift dialog operations
const openAddDialog = () => {
    isEdit.value = false;
    editId.value = null;
    form.value = {
        tenCa: '',
        gioBatDau: '08:00',
        gioKetThuc: '17:00',
        moTa: ''
    };
    showDialog.value = true;
};

const openEditDialog = (item) => {
    isEdit.value = true;
    editId.value = item.id;
    form.value = {
        tenCa: item.tenCa,
        gioBatDau: item.gioBatDau,
        gioKetThuc: item.gioKetThuc,
        moTa: item.moTa || ''
    };
    showDialog.value = true;
};

const confirmSaveShift = () => {
    if (!form.value.tenCa || !form.value.gioBatDau || !form.value.gioKetThuc) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập đầy đủ thông tin ca làm!', color: 'error' });
        return;
    }

    const modeText = isEdit.value ? 'cập nhật' : 'tạo mới';
    setConfirm({
        title: `Xác nhận ${modeText} ca làm`,
        message: `Bạn có chắc chắn muốn ${modeText} ca làm này?`,
        color: 'success',
        action: async () => {
            await saveShift();
        }
    });
};

const saveShift = async () => {
    try {
        let res;
        if (isEdit.value) {
            res = await apiService.put(`${API_LICH_LAM_VIEC.SHIFTS}/${editId.value}`, form.value);
        } else {
            res = await apiService.post(API_LICH_LAM_VIEC.SHIFTS, form.value);
        }
        
        if (res.data.success) {
            addNotification({
                title: 'Thành công',
                subtitle: isEdit.value ? 'Cập nhật ca làm thành công!' : 'Tạo ca làm thành công!',
                icon: 'CircleCheckIcon',
                color: 'success'
            });
            showDialog.value = false;
            loadData(false);
        }
    } catch (error) {
        console.error('Error saving shift:', error);
        addNotification({ title: 'Lỗi', subtitle: error.response?.data?.message || 'Có lỗi xảy ra khi lưu ca làm!', color: 'error' });
    }
};

const handleDelete = (item) => {
    setConfirm({
        title: 'Xác nhận xóa',
        message: `Bạn có chắc chắn muốn xóa ca làm "${item.tenCa}" không?`,
        color: 'error',
        action: async () => {
            try {
                const res = await apiService.delete(`${API_LICH_LAM_VIEC.SHIFTS}/${item.id}`);
                if (res.data.success) {
                    addNotification({ title: 'Thành công', subtitle: 'Xóa ca làm thành công!', icon: 'CircleCheckIcon', color: 'success' });
                    loadData(false);
                }
            } catch (error) {
                console.error('Error deleting shift:', error);
                addNotification({ title: 'Lỗi', subtitle: error.response?.data?.message || 'Có lỗi xảy ra khi xóa ca làm!', color: 'error' });
            }
        }
    });
};

onMounted(() => {
    loadData();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <AdminBreadcrumbs :items="breadcrumbs" />

        <div class="mb-2"></div>

        <div class="filter-shell">
            <AdminFilter title="Bộ lọc ca làm" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="6" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm tên ca</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Nhập tên ca..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @input="handleFilter"
                    />
                </v-col>
            </AdminFilter>
        </div>

        <AdminTable
            title="Danh mục các ca làm việc"
            :headers="tableHeaders"
            :items="paginatedItems"
            :loading="loading"
            :show-add-button="true"
            addButtonText="Tạo mới"
            class="all-center-table"
            @add="openAddDialog"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell font-weight-bold text-center">
                        <v-chip size="small" variant="flat" :class="['shift-chip', getShiftChipClass(item.tenCa)]">
                            {{ item.tenCa }}
                        </v-chip>
                    </td>
                    <td class="data-cell">{{ item.gioBatDau }}</td>
                    <td class="data-cell">{{ item.gioKetThuc }}</td>
                    <td class="data-cell text-truncate" style="max-width: 300px">{{ item.moTa }}</td>
                    <td class="data-cell action-cell">
                        <div class="action-controls">
                            <v-btn variant="text" color="primary" class="action-icon-btn" size="small" @click="openEditDialog(item)">
                                <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <v-btn variant="text" color="error" class="action-icon-btn" size="small" @click="handleDelete(item)">
                                <component :is="ADMIN_ICONS.ACTION.DELETE" size="15" />
                                <v-tooltip activator="parent" location="top">Xóa</v-tooltip>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    v-model:page-size="pagination.size"
                    :total-pages="Math.ceil(filteredItems.length / pagination.size) || 1"
                    :total-elements="filteredItems.length"
                    :current-size="paginatedItems.length"
                    @change="handleFilter"
                />
            </template>
        </AdminTable>

        <!-- Add/Edit Shift Dialog -->
        <v-dialog v-model="showDialog" max-width="500">
            <v-card class="rounded-xl pa-4">
                <v-card-title class="text-h6 font-weight-bold d-flex align-center">
                    <v-icon color="primary" class="mr-3">{{ isEdit ? 'mdi-pencil-circle' : 'mdi-plus-circle' }}</v-icon>
                    {{ isEdit ? 'Cập nhật ca làm việc' : 'Thêm ca làm việc' }}
                </v-card-title>
                <v-card-text>
                    <v-row>
                        <v-col cols="12">
                            <div class="filter-field-label">Tên ca làm</div>
                            <v-text-field
                                v-model="form.tenCa"
                                placeholder="Nhập tên ca (VD: Ca Sáng)"
                                variant="outlined"
                                density="compact"
                                hide-details
                            />
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="filter-field-label">Giờ bắt đầu</div>
                            <v-text-field v-model="form.gioBatDau" type="time" variant="outlined" density="compact" hide-details />
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="filter-field-label">Giờ kết thúc</div>
                            <v-text-field v-model="form.gioKetThuc" type="time" variant="outlined" density="compact" hide-details />
                        </v-col>
                        <v-col cols="12">
                            <div class="filter-field-label">Mô tả</div>
                            <v-textarea
                                v-model="form.moTa"
                                placeholder="Nhập mô tả ca làm..."
                                variant="outlined"
                                density="compact"
                                rows="3"
                                hide-details
                            />
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-card-actions class="pa-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" color="grey" @click="showDialog = false">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" @click="confirmSaveShift" class="px-6 rounded-lg">Lưu lại</v-btn>
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
            @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)"
        />
    </v-container>
</template>

<style scoped></style>
