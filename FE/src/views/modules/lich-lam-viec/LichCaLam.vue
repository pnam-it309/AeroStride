<script setup>
import { ref, onMounted } from 'vue';
import { AdminFilter, AdminTable, AdminPagination, AdminBreadcrumbs } from '@/components/common';
import apiService from '@/services/apiService';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';
import { ADMIN_ICONS } from '@/constants/adminIcons';

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

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch ca làm', disabled: true }
];

const tableHeaders = [
    { text: 'STT', width: '50px' },
    { text: 'Tên ca', width: '200px' },
    { text: 'Giờ bắt đầu', width: '150px' },
    { text: 'Giờ kết thúc', width: '150px' },
    { text: 'Mô tả', width: '300px' },
    { text: 'Hành động', width: '100px' }
];

const loadData = async () => {
    loading.value = true;
    try {
        const response = await apiService.get(API_LICH_LAM_VIEC.SHIFTS);
        if (response.data.success) {
            items.value = response.data.data;
            pagination.value.totalElements = items.value.length;
        }
    } catch (error) {
        console.error('Error fetching shifts:', error);
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    await loadData();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleFilter = () => {
    pagination.value.page = 1;
    loadData();
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
            :items="items"
            :loading="loading"
            addButtonText="Tạo mới"
            class="all-center-table"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell">{{ index + 1 }}</td>
                    <td class="data-cell">{{ item.tenCa }}</td>
                    <td class="data-cell">{{ item.gioBatDau }}</td>
                    <td class="data-cell">{{ item.gioKetThuc }}</td>
                    <td class="data-cell text-truncate" style="max-width: 300px">{{ item.moTa }}</td>
                    <td class="data-cell action-cell">
                        <div class="action-controls">
                            <v-btn variant="text" color="primary" class="action-icon-btn" size="small">
                                <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <v-btn variant="text" color="error" class="action-icon-btn" size="small">
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
                    :page-size="pagination.size"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    @change="loadData"
                />
            </template>
        </AdminTable>
    </v-container>
</template>
<style>

</style>