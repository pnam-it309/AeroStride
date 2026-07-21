<script setup>
import { ref, computed } from 'vue';
import { useServerPagination } from '@/composables/useServerPagination';
import { useNotifications } from '@/services/notificationService';
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { dichVuDanhGia } from '@/services/admin/dichVuDanhGia';
import { StarIcon, CheckIcon, XIcon, BanIcon, TrashIcon, SearchIcon, PhotoIcon } from 'vue-tabler-icons';
import { formatDate } from '@/utils/formatters';

const { addNotification } = useNotifications();

const filters = ref({
    keyword: '',
    trangThai: null
});

const statusOptions = [
    { title: 'Tất cả', value: null },
    { title: 'Chờ duyệt', value: 'PENDING' },
    { title: 'Đã duyệt', value: 'APPROVED' },
    { title: 'Từ chối', value: 'REJECTED' },
    { title: 'Spam', value: 'SPAM' }
];

const {
    items: reviews,
    loading,
    pagination,
    totalElements,
    totalPages,
    load: loadReviews,
    reload: reloadReviews
} = useServerPagination(
    (pageable) => dichVuDanhGia.getAll({ ...pageable, ...filters.value }),
    {
        pageSize: 10,
        onError: () => addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách đánh giá', color: 'error' })
    }
);

const tableHeaders = [
    { text: 'Khách hàng', key: 'khachHang', sortable: false, width: '200px' },
    { text: 'Sản phẩm', key: 'sanPham', sortable: false, width: '250px' },
    { text: 'Đánh giá', key: 'danhGia', sortable: false, width: '300px' },
    { text: 'Trạng thái', key: 'trangThai', sortable: false, align: 'center', width: '120px' },
    { text: 'Thời gian', key: 'ngayTao', sortable: true, width: '130px' },
    { text: 'Hành động', key: 'actions', sortable: false, align: 'center', width: '120px' }
];

const getStatusChip = (status) => {
    switch (status) {
        case 'APPROVED': return { color: 'success', text: 'Đã duyệt' };
        case 'REJECTED': return { color: 'error', text: 'Từ chối' };
        case 'SPAM': return { color: 'warning', text: 'Spam' };
        case 'PENDING':
        default: return { color: 'secondary', text: 'Chờ duyệt' };
    }
};

const handleFilter = () => reloadReviews();

const confirmDialog = ref({ show: false, title: '', message: '', id: null, action: null });
const openConfirm = (title, message, id, action) => {
    confirmDialog.value = { show: true, title, message, id, action };
};
const closeConfirm = () => confirmDialog.value.show = false;

const executeAction = async () => {
    const { id, action } = confirmDialog.value;
    try {
        if (action === 'DELETE') {
            await dichVuDanhGia.delete(id);
            addNotification({ title: 'Thành công', subtitle: 'Đã xóa đánh giá', color: 'success' });
        } else {
            await dichVuDanhGia.updateStatus(id, action);
            addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật trạng thái', color: 'success' });
        }
        closeConfirm();
        reloadReviews();
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Có lỗi xảy ra', color: 'error' });
    }
};

const parseImages = (hinhAnhStr) => {
    if (!hinhAnhStr) return [];
    try { return JSON.parse(hinhAnhStr); } catch (e) { return [hinhAnhStr]; }
};
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important">
        
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý bán hàng', disabled: false, href: '#' },
            { title: 'Quản lý đánh giá', disabled: true }
        ]" />

        <div class="d-flex align-center justify-space-between mb-4 mt-2 header-actions">
            <div class="text-h6 font-weight-bold text-slate-800">Quản lý đánh giá</div>
        </div>

        <AdminFilter title="Bộ lọc đánh giá" @filter="handleFilter" @refresh="filters = { keyword: '', trangThai: null }; reloadReviews()">
            <v-col cols="12" md="6" lg="6" class="filter-cell">
                <div class="filter-field-label">Tìm kiếm chung</div>
                <v-text-field v-model="filters.keyword" placeholder="Tên KH, SĐT, Tên sản phẩm..."
                    variant="outlined" bg-color="white" density="compact" hide-details class="compact-input"
                    @keyup.enter="handleFilter">
                    <template v-slot:prepend-inner>
                        <SearchIcon size="18" class="text-slate-400 mr-2" />
                    </template>
                </v-text-field>
            </v-col>
            <v-col cols="12" md="5" lg="4" class="filter-cell">
                <div class="filter-field-label">Trạng thái</div>
                <v-select v-model="filters.trangThai" :items="statusOptions" item-title="title" item-value="value"
                    variant="outlined" bg-color="white" density="compact" hide-details class="compact-input"
                    @update:model-value="handleFilter" />
            </v-col>
        </AdminFilter>

        <div class="admin-table-container elevation-0 border flex-grow-1 d-flex flex-column" style="min-height: 0;">
            <AdminTable title="Danh sách đánh giá" :headers="tableHeaders" :items="reviews" :total-count="totalElements"
                :loading="loading" :show-add-button="false">
                <template #row="{ item, index }">
                    <tr class="data-row">
                        <td class="data-cell">
                            <div class="d-flex flex-column">
                                <span class="font-weight-medium text-slate-800">{{ item.tenKhachHang || 'Khách vãng lai' }}</span>
                                <span class="text-caption text-slate-500">{{ item.soDienThoai || 'N/A' }}</span>
                            </div>
                        </td>
                        <td class="data-cell">
                            <div class="d-flex align-center ga-3 my-2">
                                <v-avatar size="45" rounded class="border bg-slate-50">
                                    <v-img v-if="item.hinhAnhSanPham" :src="item.hinhAnhSanPham" cover></v-img>
                                    <PhotoIcon v-else size="20" class="text-slate-400" />
                                </v-avatar>
                                <div class="d-flex flex-column text-truncate" style="max-width: 180px;">
                                    <span class="text-subtitle-2 font-weight-medium text-slate-800 text-truncate" :title="item.tenSanPham">{{ item.tenSanPham }}</span>
                                </div>
                            </div>
                        </td>
                        <td class="data-cell">
                            <div class="d-flex flex-column py-2">
                                <div class="d-flex align-center mb-1">
                                    <StarIcon v-for="i in 5" :key="i" size="14" :class="i <= item.diemDanhGia ? 'text-amber-500' : 'text-slate-200'" :fill="i <= item.diemDanhGia ? 'currentColor' : 'none'" />
                                </div>
                                <span class="text-body-2 text-slate-700" style="white-space: pre-wrap; line-height: 1.4;">{{ item.noiDung }}</span>
                                <div v-if="parseImages(item.hinhAnhDanhGia).length > 0" class="d-flex ga-2 mt-2">
                                    <v-avatar v-for="(img, idx) in parseImages(item.hinhAnhDanhGia)" :key="idx" size="36" rounded class="border">
                                        <v-img :src="img" cover></v-img>
                                    </v-avatar>
                                </div>
                            </div>
                        </td>
                        <td class="data-cell text-center">
                            <v-chip size="small" variant="flat" :color="getStatusChip(item.trangThai).color" class="font-weight-medium px-3">{{ getStatusChip(item.trangThai).text }}</v-chip>
                        </td>
                        <td class="data-cell">
                            <div class="text-body-2 text-slate-600">{{ formatDate(item.ngayTao, 'DD/MM/YYYY HH:mm') }}</div>
                        </td>
                        <td class="data-cell text-center">
                            <div class="d-flex align-center justify-center ga-1">
                                <v-btn v-if="item.trangThai === 'PENDING' || item.trangThai === 'REJECTED' || item.trangThai === 'SPAM'" icon variant="text" size="small" color="success" class="action-icon-btn" @click="openConfirm('Duyệt đánh giá', 'Bạn có chắc chắn muốn duyệt đánh giá này?', item.id, 'APPROVED')">
                                    <CheckIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Duyệt</v-tooltip>
                                </v-btn>
                                <v-btn v-if="item.trangThai === 'PENDING' || item.trangThai === 'APPROVED'" icon variant="text" size="small" color="error" class="action-icon-btn" @click="openConfirm('Từ chối đánh giá', 'Bạn có chắc chắn muốn từ chối đánh giá này?', item.id, 'REJECTED')">
                                    <XIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Từ chối</v-tooltip>
                                </v-btn>
                                <v-btn v-if="item.trangThai !== 'SPAM'" icon variant="text" size="small" color="warning" class="action-icon-btn" @click="openConfirm('Đánh dấu Spam', 'Bạn có chắc chắn muốn đánh dấu đánh giá này là SPAM?', item.id, 'SPAM')">
                                    <BanIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Đánh dấu Spam</v-tooltip>
                                </v-btn>
                                <v-btn icon variant="text" size="small" color="error" class="action-icon-btn" @click="openConfirm('Xóa đánh giá', 'Bạn có chắc chắn muốn xóa vĩnh viễn đánh giá này?', item.id, 'DELETE')">
                                    <TrashIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Xóa</v-tooltip>
                                </v-btn>
                            </div>
                        </td>
                    </tr>
                </template>
                
                <template #pagination>
                    <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                        @update:page-size="s => { pagination.size = s; reloadReviews(); }"
                        :total-pages="totalPages" :total-elements="totalElements"
                        :current-size="reviews.length" @change="loadReviews" />
                </template>
            </AdminTable>
        </div>

        <AdminConfirm v-model="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            confirm-text="Đồng ý" cancel-text="Hủy" confirm-color="primary" @confirm="executeAction"
            @cancel="closeConfirm" />
    </v-container>
</template>
