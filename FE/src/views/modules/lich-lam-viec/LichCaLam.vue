<script setup>
import { ref, onMounted } from 'vue';
import { AdminFilter, AdminTable, AdminPagination, AdminBreadcrumbs } from '@/components/common';
import apiService from '@/services/apiService';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';

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
    { text: 'STT', width: '50px', align: 'center' },
    { text: 'Tên ca', width: '200px', align: 'left' },
    { text: 'Giờ bắt đầu', width: '150px', align: 'center' },
    { text: 'Giờ kết thúc', width: '150px', align: 'center' },
    { text: 'Mô tả', width: '300px', align: 'left' },
    { text: 'Hành động', width: '100px', align: 'center' }
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
    <v-container fluid class="pa-4 animate-fade-in font-body" style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important">
        <AdminBreadcrumbs :items="breadcrumbs" />
        
        <div class="mb-2"></div>

        <div class="filter-top invoice-filter-shell">
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
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">{{ index + 1 }}</td>
                    <td class="data-cell text-left font-weight-bold">{{ item.tenCa }}</td>
                    <td class="data-cell text-center">{{ item.gioBatDau }}</td>
                    <td class="data-cell text-center">{{ item.gioKetThuc }}</td>
                    <td class="data-cell text-left text-truncate" style="max-width: 300px">{{ item.moTa }}</td>
                    <td class="data-cell text-center action-cell">
                        <div class="action-controls">
                            <v-btn icon="mdi-pencil-outline" variant="text" color="primary" class="action-icon-btn" size="small"></v-btn>
                            <v-btn icon="mdi-delete-outline" variant="text" color="error" class="action-icon-btn" size="small"></v-btn>
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
