<script setup>
/**
 * Module: Quản lý đánh giá sản phẩm (Admin)
 * View: DanhGia.vue
 * Chức năng: Duyệt, từ chối, lọc và phân loại đánh giá sản phẩm từ khách hàng.
 *            Tích hợp tự động phê duyệt và thống kê trạng thái theo Tabs.
 */
import { ref, onMounted, computed } from 'vue';
import { useAdminTable } from '@/composables/useAdminTable';
import { useNotifications } from '@/services/notificationService';
import { getBackendErrorMessage } from '@/utils/errorUtils';
import { formatDate } from '@/utils/formatters';
import { dichVuDanhGia } from '@/services/admin/dichVuDanhGia';

// Common Admin Components
import {
    AdminFilter,
    AdminTable,
    AdminPagination,
    AdminConfirm,
    AdminBreadcrumbs
} from '@/components/common';

const { addNotification } = useNotifications();

const configData = ref({
    autoApprove: true,
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
    spam: 0
});
const isUpdatingConfig = ref(false);

const ratingFilterOptions = [
    { title: 'Tất cả số sao', value: null },
    { title: '5 sao (Xuất sắc)', value: 5 },
    { title: '4 sao (Tốt)', value: 4 },
    { title: '3 sao (Bình thường)', value: 3 },
    { title: '2 sao (Kém)', value: 2 },
    { title: '1 sao (Rất tệ)', value: 1 }
];

const loadConfigAndStats = async () => {
    try {
        const res = await dichVuDanhGia.getConfig();
        if (res?.data) {
            configData.value = res.data;
        }
    } catch (e) {
        console.error('Lỗi khi tải cấu hình đánh giá:', e);
    }
};

const {
    items: reviews,
    loading,
    pagination,
    filters,
    loadData: loadReviews,
    handleFilter: handleSearch,
    handleReset
} = useAdminTable(
    async (params) => {
        const payload = {
            page: params.page,
            size: params.size,
            keyword: params.keyword || params.search || undefined,
            trangThai: params.trangThai || undefined,
            diemDanhGia: params.diemDanhGia || undefined
        };
        const res = await dichVuDanhGia.getAll(payload);
        await loadConfigAndStats();
        return res?.data || res;
    },
    {
        keyword: '',
        trangThai: null,
        diemDanhGia: null
    }
);

const handleCustomReset = () => {
    filters.value = {
        keyword: '',
        trangThai: null,
        diemDanhGia: null
    };
    handleReset();
};

const handleTabChange = (val) => {
    filters.value.trangThai = val;
    pagination.value.page = 1;
    loadReviews();
};

const handleToggleAutoApprove = async () => {
    isUpdatingConfig.value = true;
    const targetState = !configData.value.autoApprove;
    try {
        const res = await dichVuDanhGia.updateConfig(targetState);
        if (res?.data) {
            configData.value = res.data;
            addNotification({
                title: 'Thành công',
                subtitle: targetState
                    ? 'Đã BẬT tự động phê duyệt: Đánh giá hợp lệ sẽ được hiển thị ngay.'
                    : 'Đã TẮT tự động phê duyệt: Mọi đánh giá mới sẽ ở trạng thái chờ duyệt.',
                color: 'success'
            });
        }
    } catch (e) {
        addNotification({
            title: 'Lỗi',
            subtitle: getBackendErrorMessage(e, 'Không thể cập nhật cấu hình tự động duyệt'),
            color: 'error'
        });
    } finally {
        isUpdatingConfig.value = false;
    }
};

const tableHeaders = [
    { text: 'STT', width: '60px', align: 'center' },
    { text: 'Khách hàng', width: '200px', align: 'start' },
    { text: 'Sản phẩm', width: '230px', align: 'start' },
    { text: 'Đánh giá & Nhận xét', width: '340px', align: 'start' },
    { text: 'Trạng thái', width: '130px', align: 'center' },
    { text: 'Thời gian', width: '140px', align: 'center' },
    { text: 'Hành động', width: '140px', align: 'center' }
];

const getStatusChip = (status) => {
    switch (status) {
        case 'APPROVED':
            return { color: 'success', text: 'Đã duyệt', icon: 'mdi-check-circle', chipClass: 'status-chip-active' };
        case 'REJECTED':
            return { color: 'error', text: 'Từ chối', icon: 'mdi-close-circle', chipClass: 'status-chip-inactive' };
        case 'SPAM':
            return { color: 'grey-darken-1', text: 'Spam', icon: 'mdi-alert-circle', chipClass: 'status-chip-draft' };
        case 'PENDING':
        default:
            return { color: 'warning', text: 'Chờ duyệt', icon: 'mdi-clock-outline', chipClass: 'status-chip-warning' };
    }
};

const confirmDialog = ref({ show: false, title: '', message: '', id: null, action: null });
const openConfirm = (title, message, id, action) => {
    confirmDialog.value = { show: true, title, message, id, action };
};
const closeConfirm = () => (confirmDialog.value.show = false);

const executeAction = async () => {
    const { id, action } = confirmDialog.value;
    try {
        if (action === 'DELETE') {
            await dichVuDanhGia.delete(id);
            addNotification({ title: 'Thành công', subtitle: 'Đã xóa vĩnh viễn đánh giá', color: 'success' });
        } else {
            await dichVuDanhGia.updateStatus(id, action);
            const actionText = action === 'APPROVED' ? 'duyệt' : action === 'REJECTED' ? 'từ chối' : 'đánh dấu spam';
            addNotification({ title: 'Thành công', subtitle: `Đã ${actionText} đánh giá`, color: 'success' });
        }
        closeConfirm();
        loadReviews();
    } catch (e) {
        addNotification({
            title: 'Lỗi',
            subtitle: getBackendErrorMessage(e, 'Có lỗi xảy ra khi thực hiện thao tác'),
            color: 'error'
        });
    }
};

const parseImages = (hinhAnhStr) => {
    if (!hinhAnhStr) return [];
    try {
        const parsed = JSON.parse(hinhAnhStr);
        return Array.isArray(parsed) ? parsed : [parsed];
    } catch (e) {
        return [hinhAnhStr];
    }
};

// Modal preview ảnh phóng to
const previewImageModal = ref({ show: false, src: '' });
const openPreviewImage = (src) => {
    previewImageModal.value = { show: true, src };
};

onMounted(() => {
    loadConfigAndStats();
});
</script>

<template>
    <div class="pa-4 font-body admin-module-page">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý bán hàng', disabled: false, href: '#' },
                { title: 'Quản lý đánh giá', disabled: true }
            ]"
        />

        <!-- Filter Section -->
        <AdminFilter
            title="Bộ lọc đánh giá"
            @filter="handleSearch"
            @refresh="handleCustomReset"
        >
            <v-col cols="12" sm="6" md="8" class="filter-cell pb-1">
                <div class="filter-field-label">Tìm kiếm đánh giá</div>
                <v-text-field
                    v-model="filters.keyword"
                    placeholder="Tìm theo tên khách hàng, SĐT, tên sản phẩm, nội dung..."
                    variant="outlined"
                    bg-color="white"
                    density="compact"
                    hide-details
                    clearable
                    prepend-inner-icon="mdi-magnify"
                    @keyup.enter="handleSearch"
                ></v-text-field>
            </v-col>

            <v-col cols="12" sm="6" md="4" class="filter-cell pb-1">
                <div class="filter-field-label">Số sao đánh giá</div>
                <v-select
                    v-model="filters.diemDanhGia"
                    :items="ratingFilterOptions"
                    item-title="title"
                    item-value="value"
                    variant="outlined"
                    bg-color="white"
                    density="compact"
                    hide-details
                    @update:model-value="handleSearch"
                ></v-select>
            </v-col>
        </AdminFilter>

        <!-- Table Section -->
        <AdminTable
            title="Danh sách đánh giá sản phẩm"
            :headers="tableHeaders"
            :items="reviews"
            :total-count="pagination.totalElements"
            :loading="loading"
            :show-add-button="false"
        >
            <!-- Auto Approval Switch in Table Toolbar -->
            <template #extra-actions>
                <div class="d-flex align-center bg-slate-50 border rounded-lg px-3 py-1 mr-2 ga-2 auto-approve-box">
                    <v-icon size="18" color="primary">mdi-shield-check-outline</v-icon>
                    <span class="text-caption font-weight-bold text-slate-800">Tự động duyệt</span>
                    <v-switch
                        :model-value="configData.autoApprove"
                        color="primary"
                        density="compact"
                        hide-details
                        inline
                        :loading="isUpdatingConfig"
                        :disabled="isUpdatingConfig"
                        @click.stop="handleToggleAutoApprove"
                        class="auto-approve-switch"
                    />
                    <v-chip
                        size="x-small"
                        :color="configData.autoApprove ? 'primary' : 'slate-600'"
                        variant="flat"
                        class="font-weight-bold text-uppercase"
                    >
                        {{ configData.autoApprove ? 'BẬT' : 'TẮT' }}
                    </v-chip>
                </div>
            </template>

            <!-- Status Tabs above Table Header -->
            <template #top>
                <v-tabs
                    v-model="filters.trangThai"
                    bg-color="transparent"
                    color="primary"
                    show-arrows
                    grow
                    class="admin-tabs"
                    @update:model-value="handleTabChange"
                    height="48"
                >
                    <v-tab :value="null" class="text-none px-4 tab-item font-weight-medium">
                        <v-icon start size="16">mdi-view-grid-outline</v-icon>
                        Tất cả
                        <v-chip
                            size="x-small"
                            class="ml-2 font-weight-bold tab-count-chip"
                            :color="filters.trangThai === null ? 'primary' : 'slate-500'"
                            variant="tonal"
                        >
                            {{ configData.total }}
                        </v-chip>
                    </v-tab>
                    <v-tab value="PENDING" class="text-none px-4 tab-item font-weight-medium">
                        <v-icon start size="16">mdi-clock-outline</v-icon>
                        Chờ duyệt
                        <v-chip
                            size="x-small"
                            class="ml-2 font-weight-bold tab-count-chip"
                            :color="filters.trangThai === 'PENDING' ? 'primary' : 'slate-500'"
                            variant="tonal"
                        >
                            {{ configData.pending }}
                        </v-chip>
                    </v-tab>
                    <v-tab value="APPROVED" class="text-none px-4 tab-item font-weight-medium">
                        <v-icon start size="16">mdi-check-circle-outline</v-icon>
                        Đã duyệt
                        <v-chip
                            size="x-small"
                            class="ml-2 font-weight-bold tab-count-chip"
                            :color="filters.trangThai === 'APPROVED' ? 'primary' : 'slate-500'"
                            variant="tonal"
                        >
                            {{ configData.approved }}
                        </v-chip>
                    </v-tab>
                    <v-tab value="REJECTED" class="text-none px-4 tab-item font-weight-medium">
                        <v-icon start size="16">mdi-close-circle-outline</v-icon>
                        Từ chối
                        <v-chip
                            size="x-small"
                            class="ml-2 font-weight-bold tab-count-chip"
                            :color="filters.trangThai === 'REJECTED' ? 'primary' : 'slate-500'"
                            variant="tonal"
                        >
                            {{ configData.rejected }}
                        </v-chip>
                    </v-tab>
                    <v-tab value="SPAM" class="text-none px-4 tab-item font-weight-medium">
                        <v-icon start size="16">mdi-alert-octagon-outline</v-icon>
                        Spam
                        <v-chip
                            size="x-small"
                            class="ml-2 font-weight-bold tab-count-chip"
                            :color="filters.trangThai === 'SPAM' ? 'primary' : 'slate-500'"
                            variant="tonal"
                        >
                            {{ configData.spam }}
                        </v-chip>
                    </v-tab>
                </v-tabs>
            </template>

            <!-- Table Rows -->
            <template #row="{ item, index }">
                <tr class="data-row">
                    <!-- STT -->
                    <td class="data-cell text-center">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>

                    <!-- Khách hàng -->
                    <td class="data-cell">
                        <div class="d-flex align-center ga-2 text-left">
                            <v-avatar size="36" color="primary" variant="tonal" class="border">
                                <v-icon icon="mdi-account" color="primary" size="20"></v-icon>
                            </v-avatar>
                            <div class="d-flex flex-column text-left">
                                <span class="font-weight-bold text-slate-800" style="font-size: 13px">
                                    {{ item.tenKhachHang || 'Khách vãng lai' }}
                                </span>
                                <span class="text-slate-500" style="font-size: 12px">
                                    {{ item.soDienThoai || '--' }}
                                </span>
                            </div>
                        </div>
                    </td>

                    <!-- Sản phẩm -->
                    <td class="data-cell">
                        <div class="d-flex align-center ga-3 my-1 text-left">
                            <v-avatar size="44" rounded="lg" class="border bg-slate-50 flex-shrink-0">
                                <v-img v-if="item.hinhAnhSanPham" :src="item.hinhAnhSanPham" cover></v-img>
                                <v-icon v-else icon="mdi-image-outline" size="22" color="slate-400" />
                            </v-avatar>
                            <div class="d-flex flex-column text-truncate" style="max-width: 170px">
                                <span
                                    class="font-weight-medium text-slate-800 text-truncate"
                                    style="font-size: 13px"
                                    :title="item.tenSanPham"
                                >
                                    {{ item.tenSanPham || 'Sản phẩm AeroStride' }}
                                </span>
                            </div>
                        </div>
                    </td>

                    <!-- Đánh giá & Nội dung -->
                    <td class="data-cell text-left">
                        <div class="d-flex flex-column py-2">
                            <div class="d-flex align-center ga-1 mb-1">
                                <v-icon
                                    v-for="i in 5"
                                    :key="i"
                                    size="16"
                                    :color="i <= item.diemDanhGia ? '#f59e0b' : '#cbd5e1'"
                                    :icon="i <= item.diemDanhGia ? 'mdi-star' : 'mdi-star-outline'"
                                />
                                <span class="font-weight-bold ml-1 text-slate-700" style="font-size: 12px">
                                    ({{ item.diemDanhGia || 5 }}/5)
                                </span>
                            </div>
                            <span class="text-slate-700 font-normal" style="font-size: 13px; white-space: pre-wrap; line-height: 1.45;">
                                {{ item.noiDung || '(Không có lời nhận xét)' }}
                            </span>
                            <!-- Attached Images -->
                            <div v-if="parseImages(item.hinhAnhDanhGia).length > 0" class="d-flex flex-wrap ga-2 mt-2">
                                <v-avatar
                                    v-for="(img, idx) in parseImages(item.hinhAnhDanhGia)"
                                    :key="idx"
                                    size="38"
                                    rounded="md"
                                    class="border cursor-pointer hover-scale"
                                    @click="openPreviewImage(img)"
                                >
                                    <v-img :src="img" cover></v-img>
                                </v-avatar>
                            </div>
                        </div>
                    </td>

                    <!-- Trạng thái -->
                    <td class="data-cell text-center">
                        <v-chip
                            size="small"
                            variant="flat"
                            :color="getStatusChip(item.trangThai).color"
                            :class="['status-chip', getStatusChip(item.trangThai).chipClass]"
                        >
                            <v-icon start size="14" :icon="getStatusChip(item.trangThai).icon"></v-icon>
                            {{ getStatusChip(item.trangThai).text }}
                        </v-chip>
                    </td>

                    <!-- Thời gian -->
                    <td class="data-cell text-center">
                        <span class="text-slate-600" style="font-size: 13px">
                            {{ formatDate(item.ngayTao, 'DD/MM/YYYY HH:mm') }}
                        </span>
                    </td>

                    <!-- Hành động -->
                    <td class="data-cell text-center">
                        <div class="d-flex align-center justify-center ga-1 action-controls">
                            <!-- Duyệt -->
                            <v-btn
                                v-if="item.trangThai === 'PENDING' || item.trangThai === 'REJECTED' || item.trangThai === 'SPAM'"
                                icon
                                variant="text"
                                size="small"
                                color="primary"
                                class="action-icon-btn rounded-lg"
                                @click="openConfirm('Duyệt đánh giá', 'Bạn có chắc chắn muốn phê duyệt đánh giá này để hiển thị công khai?', item.id, 'APPROVED')"
                            >
                                <v-icon size="18">mdi-check-circle-outline</v-icon>
                                <v-tooltip activator="parent" location="top">Phê duyệt</v-tooltip>
                            </v-btn>

                            <!-- Từ chối -->
                            <v-btn
                                v-if="item.trangThai === 'PENDING' || item.trangThai === 'APPROVED'"
                                icon
                                variant="text"
                                size="small"
                                color="error"
                                class="action-icon-btn rounded-lg"
                                @click="openConfirm('Từ chối đánh giá', 'Bạn có chắc chắn muốn từ chối và ẩn đánh giá này?', item.id, 'REJECTED')"
                            >
                                <v-icon size="18">mdi-close-circle-outline</v-icon>
                                <v-tooltip activator="parent" location="top">Từ chối</v-tooltip>
                            </v-btn>

                            <!-- Spam -->
                            <v-btn
                                v-if="item.trangThai !== 'SPAM'"
                                icon
                                variant="text"
                                size="small"
                                color="warning"
                                class="action-icon-btn rounded-lg"
                                @click="openConfirm('Đánh dấu Spam', 'Bạn có chắc chắn muốn đánh dấu đánh giá này là SPAM?', item.id, 'SPAM')"
                            >
                                <v-icon size="18">mdi-alert-octagon-outline</v-icon>
                                <v-tooltip activator="parent" location="top">Đánh dấu Spam</v-tooltip>
                            </v-btn>

                            <!-- Xóa -->
                            <v-btn
                                icon
                                variant="text"
                                size="small"
                                color="error"
                                class="action-icon-btn rounded-lg"
                                @click="openConfirm('Xóa đánh giá', 'Bạn có chắc chắn muốn xóa vĩnh viễn đánh giá này?', item.id, 'DELETE')"
                            >
                                <v-icon size="18">mdi-trash-can-outline</v-icon>
                                <v-tooltip activator="parent" location="top">Xóa vĩnh viễn</v-tooltip>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>

            <!-- Pagination Slot -->
            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event"
                    @update:page-size="pagination.size = $event"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="reviews.length"
                    @change="loadReviews"
                />
            </template>
        </AdminTable>

        <!-- Confirm Action Modal -->
        <AdminConfirm
            v-model="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            confirm-text="Xác nhận"
            cancel-text="Hủy"
            confirm-color="primary"
            @confirm="executeAction"
            @cancel="closeConfirm"
        />

        <!-- Image Preview Modal -->
        <v-dialog v-model="previewImageModal.show" max-width="600">
            <v-card class="rounded-xl overflow-hidden bg-black">
                <v-img :src="previewImageModal.src" max-height="600" contain></v-img>
                <v-card-actions class="justify-end pa-2 bg-grey-darken-4">
                    <v-btn color="white" variant="text" @click="previewImageModal.show = false">Đóng</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </div>
</template>

<style scoped>
.auto-approve-box {
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    transition: all 0.2s ease;
}

.auto-approve-switch :deep(.v-selection-control) {
    min-height: auto;
}

.tab-count-chip {
    font-size: 11px !important;
    height: 18px !important;
    padding: 0 6px !important;
}

.hover-scale {
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hover-scale:hover {
    transform: scale(1.08);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>
