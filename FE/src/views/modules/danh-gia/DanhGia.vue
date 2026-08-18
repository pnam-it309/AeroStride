<script setup>
/**
 * Module: Quản lý đánh giá sản phẩm (Admin)
 * View: DanhGia.vue
 * Chức năng: Duyệt, từ chối, lọc và phân loại đánh giá sản phẩm từ khách hàng.
 *            Tích hợp tự động phê duyệt và thống kê trạng thái theo Tabs.
 */
import { ref, onMounted, computed } from 'vue';
import { useAdminTable } from '@/composables/useAdminTable';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useRefreshHandler } from '@/composables/useRefreshHandler';
import { useNotifications } from '@/services/notificationService';
import { getBackendErrorMessage } from '@/utils/errorUtils';
import { formatDate } from '@/utils/formatters';
import { dichVuDanhGia } from '@/services/admin/dichVuDanhGia';

// Icons
import {
    SparklesIcon,
    PhotoIcon,
    CheckIcon,
    XIcon,
    BanIcon,
    TrashIcon,
    EyeIcon
} from 'vue-tabler-icons';

// Centralized Admin Icons
import { ADMIN_ICONS } from '@/constants/adminIcons';

// Common Admin Components
import {
    AdminFilter,
    AdminTable,
    AdminPagination,
    AdminConfirm,
    AdminBreadcrumbs
} from '@/components/common';

const { addNotification } = useNotifications();
const { confirmDialog, setConfirm, clearConfirm, handleConfirm } = useConfirmDialog();
const { isRefreshing, handleRefresh: executeRefresh } = useRefreshHandler();

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
    { title: '⭐⭐⭐⭐⭐ 5 sao (Xuất sắc)', value: 5 },
    { title: '⭐⭐⭐⭐ 4 sao (Tốt)', value: 4 },
    { title: '⭐⭐⭐ 3 sao (Bình thường)', value: 3 },
    { title: '⭐⭐ 2 sao (Kém)', value: 2 },
    { title: '⭐ 1 sao (Rất tệ)', value: 1 }
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
            trangThai: (params.trangThai && params.trangThai !== 'ALL') ? params.trangThai : undefined,
            diemDanhGia: params.diemDanhGia || undefined
        };
        const res = await dichVuDanhGia.getAll(payload);
        await loadConfigAndStats();
        return res?.data || res;
    },
    {
        keyword: '',
        trangThai: 'ALL',
        diemDanhGia: null
    }
);

const handleCustomReset = () => {
    filters.value = {
        keyword: '',
        trangThai: 'ALL',
        diemDanhGia: null
    };
    pagination.value.page = 1;
    loadReviews();
};

const onRefresh = async () => {
    await executeRefresh(async () => {
        handleCustomReset();
        await loadConfigAndStats();
    });
};

const handleTabChange = (val) => {
    filters.value.trangThai = val || 'ALL';
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
    { title: 'Tất cả', value: 'ALL', count: configData.value.total, color: 'primary', icon: 'mdi-view-grid-outline' },
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
    { text: 'Thời gian', width: '130px', align: 'center' },
    { text: 'Hành động', width: '130px', align: 'center' }
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

const getIndex = (index) => {
    return (pagination.value.page - 1) * pagination.value.size + index + 1;
};

const handleUpdateStatus = (id, trangThai, title, message) => {
    setConfirm({
        title,
        message,
        color: trangThai === 'APPROVED' ? 'success' : trangThai === 'REJECTED' ? 'error' : 'warning',
        action: async () => {
            try {
                await dichVuDanhGia.updateStatus(id, trangThai);
                const actionText = trangThai === 'APPROVED' ? 'duyệt' : trangThai === 'REJECTED' ? 'từ chối' : 'đánh dấu spam';
                addNotification({ title: 'Thành công', subtitle: `Đã ${actionText} đánh giá`, color: 'success' });
                await loadReviews();
            } catch (e) {
                addNotification({
                    title: 'Lỗi',
                    subtitle: getBackendErrorMessage(e, 'Có lỗi xảy ra khi thực hiện thao tác'),
                    color: 'error'
                });
            }
        }
    });
};

const handleDelete = (id) => {
    setConfirm({
        title: 'Xóa đánh giá',
        message: 'Bạn có chắc chắn muốn xóa vĩnh viễn đánh giá này? Thao tác này không thể hoàn tác.',
        color: 'error',
        action: async () => {
            try {
                await dichVuDanhGia.delete(id);
                addNotification({ title: 'Thành công', subtitle: 'Đã xóa vĩnh viễn đánh giá', color: 'success' });
                await loadReviews();
            } catch (e) {
                addNotification({
                    title: 'Lỗi',
                    subtitle: getBackendErrorMessage(e, 'Không thể xóa đánh giá'),
                    color: 'error'
                });
            }
        }
    });
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

// Modal preview ảnh/video
const previewModal = ref({ show: false, type: 'image', src: '' });
const openPreviewImage = (src) => {
    previewModal.value = { show: true, type: 'image', src };
};
const openPreviewVideo = (src) => {
    previewModal.value = { show: true, type: 'video', src };
};

const updatePaginationSize = (size) => {
    pagination.value.size = size;
    pagination.value.page = 1;
    loadReviews();
};

onMounted(async () => {
    await Promise.all([
        loadConfigAndStats(),
        loadReviews()
    ]);
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

        <div class="mb-2"></div>

        <!-- Filter Component with Search & Rating Select -->
        <AdminFilter
            title="Bộ lọc"
            :loading="loading"
            :is-refreshing="isRefreshing"
            @refresh="onRefresh"
        >
            <v-col cols="12" sm="7" md="8" class="filter-cell pb-1">
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
                    @input="handleSearch"
                    @keyup.enter="handleSearch"
                ></v-text-field>
            </v-col>
            <v-col cols="12" sm="5" md="4" class="filter-cell pb-1">
                <div class="filter-field-label">Số sao đánh giá</div>
                <v-select
                    v-model="filters.diemDanhGia"
                    :items="ratingFilterOptions"
                    variant="outlined"
                    bg-color="white"
                    density="compact"
                    hide-details
                    class="compact-input"
                    @update:model-value="handleSearch"
                ></v-select>
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
                    <div 
                        class="auto-approve-card px-3 py-1.5 rounded-xl d-flex align-center"
                        :class="{ 'active-card': configData.autoApprove }"
                    >
                        <div class="d-flex align-center mr-3">
                            <v-avatar size="28" class="mr-2 avatar-box">
                                <SparklesIcon size="15" class="icon-spark" />
                            </v-avatar>
                            <div class="text-left">
                                <div class="font-weight-bold label-text" style="font-size: 13px; line-height: 1.2;">Tự động phê duyệt</div>
                                <div class="desc-text" style="font-size: 11px !important; line-height: 1.1; margin-top: 1px;">
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
                            class="ml-auto tight-switch"
                            :loading="isUpdatingConfig"
                            :disabled="isUpdatingConfig"
                            @click.prevent.stop="handleToggleAutoApprove"
                        ></v-switch>
                    </div>
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
                        height="48"
                    >
                        <v-tab
                            v-for="tab in statusTabs"
                            :key="tab.title"
                            :value="tab.value"
                            class="text-none px-4 tab-item"
                        >
                            <v-icon start size="16" class="mr-1">{{ tab.icon }}</v-icon>
                            {{ tab.title }}
                            <v-chip
                                v-if="tab.count > 0"
                                size="x-small"
                                :color="filters.trangThai === tab.value ? 'primary' : tab.color"
                                variant="flat"
                                class="ml-2 font-weight-bold"
                            >
                                {{ tab.count }}
                            </v-chip>
                        </v-tab>
                    </v-tabs>
                </template>

                <template #row="{ item, index }">
                    <tr class="data-row">
                        <!-- 1. STT -->
                        <td class="data-cell text-center font-weight-medium text-slate-600">
                            {{ getIndex(index) }}
                        </td>

                        <!-- 2. Khách hàng -->
                        <td class="data-cell text-left px-3">
                            <div class="d-flex align-center ga-3">
                                <v-avatar size="36" color="indigo-lighten-5" class="border flex-shrink-0">
                                    <v-icon icon="mdi-account" color="indigo-darken-2" size="18"></v-icon>
                                </v-avatar>
                                <div class="d-flex flex-column text-truncate" style="max-width: 145px">
                                    <span class="font-weight-semibold text-slate-800 text-truncate" :title="item.tenKhachHang" style="font-size: 13px">
                                        {{ item.tenKhachHang || 'Khách vãng lai' }}
                                    </span>
                                    <span class="text-caption text-slate-500 text-truncate" :title="item.soDienThoai" style="font-size: 11.5px">
                                        <v-icon size="12" class="mr-0.5 text-slate-400">mdi-phone</v-icon>{{ item.soDienThoai || 'Chưa có SĐT' }}
                                    </span>
                                </div>
                            </div>
                        </td>

                        <!-- 3. Sản phẩm -->
                        <td class="data-cell text-left px-3">
                            <div class="d-flex align-center ga-3 my-1">
                                <v-avatar size="44" rounded="lg" class="border bg-slate-50 flex-shrink-0">
                                    <v-img v-if="item.hinhAnhSanPham" :src="item.hinhAnhSanPham" cover></v-img>
                                    <PhotoIcon v-else size="20" class="text-slate-400" />
                                </v-avatar>
                                <div class="d-flex flex-column text-truncate" style="max-width: 165px">
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

                        <!-- 4. Đánh giá & Nhận xét -->
                        <td class="data-cell text-left px-3 py-2" style="min-width: 280px">
                            <div class="d-flex flex-column">
                                <!-- Star rating -->
                                <div class="d-flex align-center ga-1 mb-1">
                                    <v-icon
                                        v-for="i in 5"
                                        :key="i"
                                        size="15"
                                        :color="i <= (item.diemDanhGia || 5) ? '#f59e0b' : '#cbd5e1'"
                                        :icon="i <= (item.diemDanhGia || 5) ? 'mdi-star' : 'mdi-star-outline'"
                                    />
                                    <span class="font-weight-bold ml-1 text-slate-700" style="font-size: 12px">
                                        ({{ item.diemDanhGia || 5 }}/5)
                                    </span>
                                </div>

                                <!-- Review Content -->
                                <div class="text-slate-700 review-content-clamp" :title="item.noiDung" style="font-size: 13px; line-height: 1.45;">
                                    {{ item.noiDung || '(Không có nội dung nhận xét)' }}
                                </div>

                                <!-- Attached Images / Video -->
                                <div v-if="parseImages(item.hinhAnhDanhGia).length > 0 || item.videoDanhGia" class="d-flex align-center flex-wrap ga-2 mt-2">
                                    <v-avatar
                                        v-for="(img, idx) in parseImages(item.hinhAnhDanhGia)"
                                        :key="idx"
                                        size="36"
                                        rounded="md"
                                        class="border cursor-pointer hover-scale flex-shrink-0"
                                        @click.stop="openPreviewImage(img)"
                                    >
                                        <v-img :src="img" cover></v-img>
                                    </v-avatar>
                                    <v-chip
                                        v-if="item.videoDanhGia"
                                        size="x-small"
                                        color="primary"
                                        variant="tonal"
                                        prepend-icon="mdi-video"
                                        class="cursor-pointer"
                                        @click.stop="openPreviewVideo(item.videoDanhGia)"
                                    >
                                        Video
                                    </v-chip>
                                </div>
                            </div>
                        </td>

                        <!-- 5. Trạng thái -->
                        <td class="data-cell text-center">
                            <v-chip
                                size="small"
                                variant="flat"
                                :color="getStatusChip(item.trangThai).color"
                                class="font-weight-medium px-3 text-none justify-center"
                                :class="getStatusChip(item.trangThai).chipClass"
                            >
                                <v-icon start size="13" :icon="getStatusChip(item.trangThai).icon"></v-icon>
                                {{ getStatusChip(item.trangThai).text }}
                            </v-chip>
                        </td>

                        <!-- 6. Thời gian -->
                        <td class="data-cell text-center px-2">
                            <div class="text-slate-700 font-weight-medium" style="font-size: 12.5px">
                                {{ formatDate(item.ngayTao, 'dd/MM/yyyy') }}
                            </div>
                            <div class="text-caption text-slate-400" style="font-size: 11px">
                                {{ formatDate(item.ngayTao, 'HH:mm') }}
                            </div>
                        </td>

                        <!-- 7. Hành động -->
                        <td class="data-cell text-center action-cell px-2">
                            <div class="d-flex align-center justify-center action-controls">
                                <!-- Duyệt -->
                                <v-btn
                                    v-if="item.trangThai !== 'APPROVED'"
                                    icon
                                    variant="text"
                                    size="28"
                                    class="rounded-lg action-icon-btn"
                                    color="success"
                                    @click.stop="handleUpdateStatus(item.id, 'APPROVED', 'Phê duyệt đánh giá', 'Bạn có chắc chắn muốn phê duyệt đánh giá này để hiển thị công khai trên cửa hàng?')"
                                >
                                    <CheckIcon size="15" />
                                    <v-tooltip activator="parent" location="top">Phê duyệt</v-tooltip>
                                </v-btn>

                                <!-- Từ chối -->
                                <v-btn
                                    v-if="item.trangThai !== 'REJECTED'"
                                    icon
                                    variant="text"
                                    size="28"
                                    class="rounded-lg action-icon-btn"
                                    color="error"
                                    @click.stop="handleUpdateStatus(item.id, 'REJECTED', 'Từ chối đánh giá', 'Bạn có chắc chắn muốn từ chối và ẩn đánh giá này khỏi cửa hàng?')"
                                >
                                    <XIcon size="15" />
                                    <v-tooltip activator="parent" location="top">Từ chối</v-tooltip>
                                </v-btn>

                                <!-- Đánh dấu Spam -->
                                <v-btn
                                    v-if="item.trangThai !== 'SPAM'"
                                    icon
                                    variant="text"
                                    size="28"
                                    class="rounded-lg action-icon-btn"
                                    color="warning"
                                    @click.stop="handleUpdateStatus(item.id, 'SPAM', 'Đánh dấu Spam', 'Bạn có chắc chắn muốn đánh dấu đánh giá này là SPAM rác?')"
                                >
                                    <BanIcon size="15" />
                                    <v-tooltip activator="parent" location="top">Đánh dấu Spam</v-tooltip>
                                </v-btn>

                                <!-- Xóa vĩnh viễn -->
                                <v-btn
                                    icon
                                    variant="text"
                                    size="28"
                                    class="rounded-lg action-icon-btn"
                                    color="error"
                                    @click.stop="handleDelete(item.id)"
                                >
                                    <component :is="ADMIN_ICONS.ACTION.DELETE" size="15" />
                                    <v-tooltip activator="parent" location="top">Xóa vĩnh viễn</v-tooltip>
                                </v-btn>
                            </div>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination
                        v-model="pagination.page"
                        :page-size="pagination.size"
                        @update:page-size="updatePaginationSize"
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
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)"
        />

        <!-- Media Preview Modal -->
        <v-dialog v-model="previewModal.show" max-width="650">
            <v-card class="rounded-2xl overflow-hidden bg-black elevation-8">
                <div v-if="previewModal.type === 'image'" class="d-flex align-center justify-center" style="min-height: 300px; max-height: 80vh;">
                    <v-img :src="previewModal.src" max-height="75vh" contain></v-img>
                </div>
                <div v-else-if="previewModal.type === 'video'" class="pa-2 d-flex align-center justify-center">
                    <video :src="previewModal.src" controls autoplay style="max-width: 100%; max-height: 70vh; border-radius: 8px;"></video>
                </div>
                <v-card-actions class="justify-end pa-3 bg-grey-darken-4">
                    <v-btn color="white" variant="text" prepend-icon="mdi-close" @click="previewModal.show = false">Đóng</v-btn>
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

:deep(.v-field),
:deep(.v-field__outline) {
    border-radius: 12px !important;
}

.hover-scale {
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hover-scale:hover {
    transform: scale(1.08);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.review-content-clamp {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    word-break: break-word;
}

.auto-approve-card {
    border: 1px solid #e2e8f0 !important;
    background-color: #ffffff;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    min-width: 260px;
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
    box-shadow: 0 2px 8px rgba(16, 185, 129, 0.08) !important;
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

.tight-switch {
    transform: scale(0.85);
}
</style>
