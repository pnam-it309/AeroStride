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

const statusTabs = computed(() => [
    { title: 'Tất cả', value: null, count: configData.value.total, color: 'primary', icon: 'mdi-view-grid-outline' },
    { title: 'Chờ duyệt', value: 'PENDING', count: configData.value.pending, color: 'warning', icon: 'mdi-clock-outline' },
    { title: 'Đã duyệt', value: 'APPROVED', count: configData.value.approved, color: 'success', icon: 'mdi-check-circle-outline' },
    { title: 'Từ chối', value: 'REJECTED', count: configData.value.rejected, color: 'error', icon: 'mdi-close-circle-outline' },
    { title: 'Spam', value: 'SPAM', count: configData.value.spam, color: 'grey-darken-1', icon: 'mdi-alert-circle-outline' }
]);


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

        <!-- Filter Component with Search -->
        <AdminFilter
            title="Bộ lọc"
            @filter="handleFilter"
            @refresh="
                handleCustomReset();
                loadConfigAndStats();
            "
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
        </AdminFilter>

        <!-- Table Container -->
        <div class="admin-table-container elevation-0 border flex-grow-1 d-flex flex-column" style="min-height: 0">
            <AdminTable
                title="Danh sách đánh giá sản phẩm"
                :headers="tableHeaders"
                :items="reviews"
                :total-count="pagination.totalElements"
                :loading="loading"
                :show-add-button="false"
            >
                <template #extra-actions>
                    <!-- Auto-Approval Toggle Switch Card -->
                    <v-card 
                        elevation="0" 
                        class="auto-approve-card px-4 py-1.5 rounded-xl d-flex align-center"
                        :class="{ 'active-card': configData.autoApprove }"
                        style="max-height: 40px; border: 1px solid #e2e8f0 !important;"
                    >
                        <div class="d-flex align-center mr-4">
                            <v-avatar size="26" class="mr-2 avatar-box">
                                <SparklesIcon size="13" class="icon-spark" />
                            </v-avatar>
                            <div class="text-left">
                                <div class="font-weight-semibold label-text" style="font-size: 13px; line-height: 1.2;">Tự động phê duyệt</div>
                                <div class="desc-text" style="font-size: 10px !important; line-height: 1.1; margin-top: 1px;">
                                    {{ configData.autoApprove 
                                        ? 'Hiển thị ngay lập tức' 
                                        : 'Đánh giá chờ duyệt' }}
                                </div>
                            </div>
                        </div>
                        <v-switch
                            :model-value="configData.autoApprove"
                            color="success"
                            hide-details
                            density="compact"
                            class="ml-auto"
                            :loading="isUpdatingConfig"
                            :disabled="isUpdatingConfig"
                            @click.stop="handleToggleAutoApprove"
                        ></v-switch>
                    </v-card>
                </template>
                <template #top>
                    <v-tabs
                        v-model="filters.trangThai"
                        bg-color="transparent"
                        color="primary"
                        show-arrows
                        grow
                        class="admin-tabs"
                        @update:model-value="handleTabChange"
                        height="54"
                    >
                        <v-tab
                            v-for="tab in statusTabs"
                            :key="tab.title"
                            :value="tab.value"
                            class="text-none px-4 tab-item"
                        >
                            <v-icon start size="16" class="mr-1">{{ tab.icon }}</v-icon>
                            {{ tab.title }}
                            <v-badge
                                v-if="tab.count > 0"
                                :content="tab.count"
                                inline
                                :color="filters.trangThai === tab.value ? 'primary' : tab.color"
                                class="ml-2 font-weight-bold"
                            />
                        </v-tab>
                    </v-tabs>
                </template>
                <template #row="{ item }">
                    <tr class="data-row">
                        <!-- Cột Khách hàng -->
                        <td class="data-cell">
                            <div class="d-flex align-center ga-2">
                                <v-avatar size="38" color="indigo-lighten-5" class="border">
                                    <v-icon icon="mdi-account" color="indigo-darken-3" size="20"></v-icon>
                                </v-avatar>
                                <div class="d-flex flex-column text-left">
                                    <span class="font-weight-bold text-slate-800" style="font-size: 13px">{{ item.tenKhachHang || 'Khách vãng lai' }}</span>
                                    <span class="text-slate-500" style="font-size: 12px">{{ item.soDienThoai || 'Chưa có SĐT' }}</span>
                                </div>
                            </div>
                        </td>

                        <!-- Cột Sản phẩm -->
                        <td class="data-cell">
                            <div class="d-flex align-center ga-3 my-2 text-left">
                                <v-avatar size="48" rounded="lg" class="border bg-slate-50 flex-shrink-0">
                                    <v-img v-if="item.hinhAnhSanPham" :src="item.hinhAnhSanPham" cover></v-img>
                                    <PhotoIcon v-else size="22" class="text-slate-400" />
                                </v-avatar>
                                <div class="d-flex flex-column text-truncate" style="max-width: 200px">
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

                        <!-- Cột Đánh giá & Nội dung -->
                        <td class="data-cell text-left">
                            <div class="d-flex flex-column py-2">
                                <div class="d-flex align-center ga-1 mb-1">
                                    <StarIcon
                                        v-for="i in 5"
                                        :key="i"
                                        size="16"
                                        :class="i <= item.diemDanhGia ? 'text-amber-500' : 'text-slate-200'"
                                        :fill="i <= item.diemDanhGia ? 'currentColor' : 'none'"
                                    />
                                    <span class="font-weight-bold ml-1 text-slate-700" style="font-size: 12px">({{ item.diemDanhGia || 5 }}/5)</span>
                                </div>
                                <span class="text-slate-700 font-normal" style="font-size: 13px; white-space: pre-wrap; line-height: 1.45;">
                                    {{ item.noiDung || '(Không có lời nhận xét)' }}
                                </span>
                                <!-- Attached Images -->
                                <div v-if="parseImages(item.hinhAnhDanhGia).length > 0" class="d-flex flex-wrap ga-2 mt-2">
                                    <v-avatar
                                        v-for="(img, idx) in parseImages(item.hinhAnhDanhGia)"
                                        :key="idx"
                                        size="40"
                                        rounded="md"
                                        class="border cursor-pointer hover-scale"
                                        @click="openPreviewImage(img)"
                                    >
                                        <v-img :src="img" cover></v-img>
                                    </v-avatar>
                                </div>
                            </div>
                        </td>

                        <!-- Cột Trạng thái -->
                        <td class="data-cell text-center">
                            <v-chip
                                size="small"
                                variant="flat"
                                :color="getStatusChip(item.trangThai).color"
                                class="font-weight-medium px-3 text-none"
                            >
                                <v-icon start size="14" :icon="getStatusChip(item.trangThai).icon"></v-icon>
                                {{ getStatusChip(item.trangThai).text }}
                            </v-chip>
                        </td>

                        <!-- Cột Thời gian -->
                        <td class="data-cell">
                            <div class="text-slate-600" style="font-size: 13px">{{ formatDate(item.ngayTao, 'DD/MM/YYYY HH:mm') }}</div>
                        </td>

                        <!-- Cột Thao tác -->
                        <td class="data-cell text-center">
                            <div class="d-flex align-center justify-center ga-1">
                                <!-- Duyệt -->
                                <v-btn
                                    v-if="item.trangThai === 'PENDING' || item.trangThai === 'REJECTED' || item.trangThai === 'SPAM'"
                                    icon
                                    variant="text"
                                    size="small"
                                    color="success"
                                    class="action-icon-btn"
                                    @click="openConfirm('Duyệt đánh giá', 'Bạn có chắc chắn muốn phê duyệt đánh giá này để hiển thị công khai?', item.id, 'APPROVED')"
                                >
                                    <CheckIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Phê duyệt</v-tooltip>
                                </v-btn>

                                <!-- Từ chối -->
                                <v-btn
                                    v-if="item.trangThai === 'PENDING' || item.trangThai === 'APPROVED'"
                                    icon
                                    variant="text"
                                    size="small"
                                    color="error"
                                    class="action-icon-btn"
                                    @click="openConfirm('Từ chối đánh giá', 'Bạn có chắc chắn muốn từ chối và ẩn đánh giá này?', item.id, 'REJECTED')"
                                >
                                    <XIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Từ chối</v-tooltip>
                                </v-btn>

                                <!-- Spam -->
                                <v-btn
                                    v-if="item.trangThai !== 'SPAM'"
                                    icon
                                    variant="text"
                                    size="small"
                                    color="warning"
                                    class="action-icon-btn"
                                    @click="openConfirm('Đánh dấu Spam', 'Bạn có chắc chắn muốn đánh dấu đánh giá này là SPAM?', item.id, 'SPAM')"
                                >
                                    <BanIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Đánh dấu Spam</v-tooltip>
                                </v-btn>

                                <!-- Xóa -->
                                <v-btn
                                    icon
                                    variant="text"
                                    size="small"
                                    color="error"
                                    class="action-icon-btn"
                                    @click="openConfirm('Xóa đánh giá', 'Bạn có chắc chắn muốn xóa vĩnh viễn đánh giá này?', item.id, 'DELETE')"
                                >
                                    <TrashIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Xóa vĩnh viễn</v-tooltip>
                                </v-btn>
                            </div>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination
                        v-model="pagination.page"
                        v-model:page-size="pagination.size"
                        :total-pages="pagination.totalPages"
                        :total-elements="pagination.totalElements"
                        :current-size="reviews.length"
                        @change="loadReviews"
                    />
                </template>
            </AdminTable>
        </div>

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
:deep(.compact-input) .v-field__input,
:deep(.compact-input) input,
:deep(.compact-input) input::placeholder {
    font-size: 13px !important;
}

.hover-scale {
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hover-scale:hover {
    transform: scale(1.08);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.auto-approve-card {
    border: 1px solid #e2e8f0 !important;
    background-color: #ffffff;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    min-width: 320px;
}

.auto-approve-card .avatar-box {
    background-color: #f1f5f9;
    color: #94a3b8;
    transition: all 0.3s ease;
}

.auto-approve-card .icon-spark {
    transition: all 0.3s ease;
}

.auto-approve-card .label-text {
    color: #1e293b;
    transition: all 0.3s ease;
}

.auto-approve-card .desc-text {
    color: #64748b;
    font-weight: 500;
    transition: all 0.3s ease;
}

/* Active State */
.auto-approve-card.active-card {
    border-color: #10b981 !important;
    background-color: #f0fdf4;
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.08) !important;
}

.auto-approve-card.active-card .avatar-box {
    background-color: #d1fae5;
    color: #10b981;
}

.auto-approve-card.active-card .label-text {
    color: #065f46 !important;
}

.auto-approve-card.active-card .desc-text {
    color: #047857 !important;
}
</style>
