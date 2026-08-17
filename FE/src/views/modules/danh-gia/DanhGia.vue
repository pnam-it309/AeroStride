<script setup>
import { ref, onMounted, computed } from 'vue';
import { useServerPagination } from '@/composables/useServerPagination';
import { useNotifications } from '@/services/notificationService';
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { dichVuDanhGia } from '@/services/admin/dichVuDanhGia';
import { 
    StarIcon, 
    CheckIcon, 
    XIcon, 
    BanIcon, 
    TrashIcon, 
    PhotoIcon, 
    SearchIcon,
    SparklesIcon,
    EyeIcon
} from 'vue-tabler-icons';
import { formatDate } from '@/utils/formatters';

const { addNotification } = useNotifications();

const filters = ref({
    keyword: '',
    trangThai: null
});

const configData = ref({
    autoApprove: true,
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
    spam: 0
});
const isUpdatingConfig = ref(false);

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
        addNotification({ title: 'Lỗi', subtitle: 'Không thể cập nhật cấu hình tự động duyệt', color: 'error' });
    } finally {
        isUpdatingConfig.value = false;
    }
};

const statusTabs = computed(() => [
    { title: 'Tất cả', value: null, count: configData.value.total, color: 'primary' },
    { title: 'Chờ duyệt', value: 'PENDING', count: configData.value.pending, color: 'warning' },
    { title: 'Đã duyệt', value: 'APPROVED', count: configData.value.approved, color: 'success' },
    { title: 'Từ chối', value: 'REJECTED', count: configData.value.rejected, color: 'error' },
    { title: 'Spam', value: 'SPAM', count: configData.value.spam, color: 'grey' }
]);

const selectStatusTab = (val) => {
    filters.value.trangThai = val;
    reloadReviews();
};

const {
    items: reviews,
    loading,
    pagination,
    totalElements,
    totalPages,
    load: loadReviews,
    reload: reloadReviews
} = useServerPagination((pageable) => dichVuDanhGia.getAll({ ...pageable, ...filters.value }), {
    pageSize: 10,
    onError: () => addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách đánh giá', color: 'error' })
});

const tableHeaders = [
    { text: 'Khách hàng', key: 'khachHang', sortable: false, width: '220px' },
    { text: 'Sản phẩm', key: 'sanPham', sortable: false, width: '260px' },
    { text: 'Đánh giá & Nhận xét', key: 'danhGia', sortable: false, width: '360px' },
    { text: 'Trạng thái', key: 'trangThai', sortable: false, align: 'center', width: '130px' },
    { text: 'Thời gian', key: 'ngayTao', sortable: true, width: '140px' },
    { text: 'Hành động', key: 'actions', sortable: false, align: 'center', width: '130px' }
];

const getStatusChip = (status) => {
    switch (status) {
        case 'APPROVED':
            return { color: 'success', text: 'Đã duyệt', icon: 'mdi-check-circle' };
        case 'REJECTED':
            return { color: 'error', text: 'Từ chối', icon: 'mdi-close-circle' };
        case 'SPAM':
            return { color: 'grey-darken-1', text: 'Spam', icon: 'mdi-alert-circle' };
        case 'PENDING':
        default:
            return { color: 'warning', text: 'Chờ duyệt', icon: 'mdi-clock-outline' };
    }
};

const handleFilter = () => reloadReviews();

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
        reloadReviews();
        loadConfigAndStats();
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Có lỗi xảy ra khi thực hiện thao tác', color: 'error' });
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
    <v-container
        fluid
        class="pa-4 animate-fade-in font-body admin-module-page"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important"
    >
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý bán hàng', disabled: false, href: '#' },
                { title: 'Quản lý đánh giá', disabled: true }
            ]"
        />

        <!-- Top Header & Auto-Approval Banner Card -->
        <div class="d-flex flex-wrap align-center justify-space-between mb-4 mt-2 gap-3 header-actions">
            <div>
                <div class="font-weight-bold text-slate-800" style="font-size: 18px">Quản lý đánh giá sản phẩm</div>
                <div class="text-slate-500" style="font-size: 13px">Kiểm duyệt, quản lý nhận xét và tự động phê duyệt đánh giá hợp lệ</div>
            </div>

            <!-- Auto-Approval Toggle Switch Card -->
            <v-card 
                elevation="0" 
                class="border px-4 py-2 rounded-xl d-flex align-center bg-white"
                :style="configData.autoApprove ? 'border-color: #10b981 !important;' : ''"
            >
                <div class="d-flex align-center mr-4">
                    <v-avatar size="34" :color="configData.autoApprove ? 'emerald-lighten-5' : 'slate-100'" class="mr-2">
                        <SparklesIcon size="18" :class="configData.autoApprove ? 'text-emerald-600' : 'text-slate-400'" />
                    </v-avatar>
                    <div>
                        <div class="d-flex align-center">
                            <span class="font-weight-bold text-slate-800" style="font-size: 14px">Tự động phê duyệt</span>
                            <v-chip 
                                size="x-small" 
                                :color="configData.autoApprove ? 'success' : 'secondary'" 
                                class="ml-2 font-weight-bold text-uppercase"
                                variant="flat"
                            >
                                {{ configData.autoApprove ? 'Đang BẬT' : 'Đang TẮT' }}
                            </v-chip>
                        </div>
                        <div class="text-slate-500" style="font-size: 12px !important;">
                            {{ configData.autoApprove 
                                ? 'Đánh giá hợp lệ sẽ hiển thị ngay lập tức' 
                                : 'Mọi đánh giá phải chờ Admin duyệt thủ công' }}
                        </div>
                    </div>
                </div>
                <v-switch
                    :model-value="configData.autoApprove"
                    color="success"
                    hide-details
                    density="compact"
                    :loading="isUpdatingConfig"
                    :disabled="isUpdatingConfig"
                    @click.stop="handleToggleAutoApprove"
                ></v-switch>
            </v-card>
        </div>

        <!-- Filter Component with Tabs & Search -->
        <AdminFilter
            title="Bộ lọc đánh giá"
            @filter="handleFilter"
            @refresh="
                filters = { keyword: '', trangThai: null };
                reloadReviews();
                loadConfigAndStats();
            "
        >
            <v-col cols="12" md="6" lg="5" class="filter-cell">
                <div class="filter-field-label">Tìm kiếm đánh giá</div>
                <v-text-field
                    v-model="filters.keyword"
                    placeholder="Tên khách hàng, SĐT, tên sản phẩm, nội dung..."
                    variant="outlined"
                    bg-color="white"
                    density="compact"
                    hide-details
                    class="compact-input"
                    clearable
                    @keyup.enter="handleFilter"
                >
                    <template v-slot:prepend-inner>
                        <SearchIcon size="18" class="text-slate-400 mr-2" />
                    </template>
                </v-text-field>
            </v-col>

            <v-col cols="12" md="6" lg="7" class="filter-cell d-flex align-center">
                <div class="d-flex flex-wrap ga-2 mt-4">
                    <v-btn
                        v-for="tab in statusTabs"
                        :key="tab.title"
                        size="small"
                        :variant="filters.trangThai === tab.value ? 'flat' : 'outlined'"
                        :color="filters.trangThai === tab.value ? 'primary' : 'slate-600'"
                        class="text-none font-weight-medium rounded-pill px-3"
                        @click="selectStatusTab(tab.value)"
                    >
                        {{ tab.title }}
                        <v-badge
                            v-if="tab.count > 0"
                            :content="tab.count"
                            inline
                            :color="filters.trangThai === tab.value ? 'white' : tab.color"
                            :text-color="filters.trangThai === tab.value ? 'primary' : 'white'"
                            class="ml-1 font-weight-bold"
                        />
                    </v-btn>
                </div>
            </v-col>
        </AdminFilter>

        <!-- Table Container -->
        <div class="admin-table-container elevation-0 border flex-grow-1 d-flex flex-column" style="min-height: 0">
            <AdminTable
                title="Danh sách đánh giá sản phẩm"
                :headers="tableHeaders"
                :items="reviews"
                :total-count="totalElements"
                :loading="loading"
                :show-add-button="false"
            >
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
                        :page-size="pagination.size"
                        @update:page-size="
                            (s) => {
                                pagination.size = s;
                                reloadReviews();
                            }
                        "
                        :total-pages="totalPages"
                        :total-elements="totalElements"
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
    </v-container>
</template>

<style scoped>
.hover-scale {
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.hover-scale:hover {
    transform: scale(1.08);
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
</style>
