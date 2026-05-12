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
    search: '',
    ngay: null
});

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch sử hoạt động', disabled: true }
];

const tableHeaders = [
    { text: 'STT', width: '50px', align: 'center' },
    { text: 'Người thực hiện', width: '150px', align: 'left' },
    { text: 'Hành động', width: '250px', align: 'left' },
    { text: 'Đối tượng', width: '150px', align: 'left' },
    { text: 'Thời gian', width: '180px', align: 'center' }
];

const loadData = async () => {
    loading.value = true;
    try {
        const response = await apiService.get(API_LICH_LAM_VIEC.ACTIVITIES, {
            params: {
                page: pagination.value.page - 1,
                size: pagination.value.size
            }
        });
        if (response.data.success) {
            items.value = response.data.data.content;
            pagination.value.totalElements = response.data.data.totalElements;
            pagination.value.totalPages = response.data.data.totalPages;
        }
    } catch (error) {
        console.error('Error fetching activities:', error);
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
            <AdminFilter title="Bộ lọc lịch sử" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="6" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm hoạt động</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Nhập tên người thực hiện, hành động..."
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
            title="Lịch sử hoạt động hệ thống"
            :headers="tableHeaders"
            :items="items"
            :loading="loading"
            :show-add-button="false"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell text-left">
                        <div class="d-flex align-center">
                            <v-avatar size="24" color="primary" class="mr-2 text-white text-caption">
                                {{ item.nguoiThucHien ? item.nguoiThucHien.charAt(0) : '?' }}
                            </v-avatar>
                            <span class="font-weight-bold">{{ item.nguoiThucHien }}</span>
                        </div>
                    </td>
                    <td class="data-cell text-left">{{ item.hanhDong }}</td>
                    <td class="data-cell text-left">
                        <v-chip size="x-small" variant="outlined">{{ item.doiTuong }}</v-chip>
                    </td>
                    <td class="data-cell text-center text-slate-500 text-caption">{{ item.ngay }}</td>
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
