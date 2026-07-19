<template>
    <div class="danh-gia-management">
        <AdminBreadcrumbs :breadcrumbs="breadcrumbs" />

        <v-card class="mb-4" elevation="0" border>
            <v-card-text>
                <v-row>
                    <v-col cols="12" md="6">
                        <v-text-field
                            v-model="filter.keyword"
                            label="Tìm kiếm khách hàng, SĐT, tên sản phẩm..."
                            density="compact"
                            variant="outlined"
                            hide-details
                            clearable
                            prepend-inner-icon="mdi-magnify"
                            @keyup.enter="handleSearch"
                            @click:clear="handleSearch"
                        ></v-text-field>
                    </v-col>
                    <v-col cols="12" md="4">
                        <v-select
                            v-model="filter.trangThai"
                            :items="statusOptions"
                            label="Trạng thái"
                            density="compact"
                            variant="outlined"
                            hide-details
                            @update:modelValue="handleSearch"
                        ></v-select>
                    </v-col>
                </v-row>
            </v-card-text>
        </v-card>

        <AdminTable
            :headers="headers"
            :items="items"
            :loading="loading"
            hide-default-footer
        >
            <template #item.ngayTao="{ item }">
                {{ formatDate(item.ngayTao) }}
            </template>
            <template #item.khachHang="{ item }">
                <div class="font-weight-bold">{{ item.tenKhachHang || 'Khách vãng lai' }}</div>
                <div class="text-caption text-grey">{{ item.soDienThoai }}</div>
            </template>
            <template #item.sanPham="{ item }">
                <div class="d-flex align-center">
                    <v-avatar rounded="lg" size="48" class="mr-3" border>
                        <v-img :src="item.hinhAnhSanPham" cover></v-img>
                    </v-avatar>
                    <div class="text-truncate" style="max-width: 200px;" :title="item.tenSanPham">
                        {{ item.tenSanPham }}
                    </div>
                </div>
            </template>
            <template #item.diemDanhGia="{ item }">
                <v-rating
                    :model-value="item.diemDanhGia"
                    color="warning"
                    density="compact"
                    half-increments
                    readonly
                    size="small"
                ></v-rating>
            </template>
            <template #item.noiDung="{ item }">
                <div class="text-truncate" style="max-width: 250px;" :title="item.noiDung">
                    {{ item.noiDung }}
                </div>
            </template>
            <template #item.trangThai="{ item }">
                <v-chip :color="getStatusColor(item.trangThai)" size="small" class="font-weight-medium">
                    {{ getStatusText(item.trangThai) }}
                </v-chip>
            </template>
            <template #item.actions="{ item }">
                <div class="d-flex gap-2">
                    <v-btn
                        v-if="item.trangThai === 'PENDING' || item.trangThai === 'REJECTED' || item.trangThai === 'SPAM'"
                        color="success"
                        variant="tonal"
                        size="small"
                        icon="mdi-check"
                        @click="updateStatus(item, 'APPROVED')"
                        title="Duyệt"
                    ></v-btn>
                    <v-btn
                        v-if="item.trangThai === 'PENDING' || item.trangThai === 'APPROVED'"
                        color="error"
                        variant="tonal"
                        size="small"
                        icon="mdi-close"
                        @click="updateStatus(item, 'REJECTED')"
                        title="Từ chối"
                    ></v-btn>
                    <v-btn
                        color="error"
                        variant="text"
                        size="small"
                        icon="mdi-delete"
                        @click="confirmDelete(item)"
                        title="Xóa"
                    ></v-btn>
                </div>
            </template>
        </AdminTable>

        <AdminPagination
            v-if="totalItems > 0"
            :page="page"
            :total-pages="totalPages"
            :total-items="totalItems"
            :page-size="size"
            @update:page="handlePageChange"
        />

        <AdminConfirm
            v-model="deleteDialog"
            title="Xóa đánh giá"
            content="Bạn có chắc chắn muốn xóa đánh giá này không? Hành động này không thể hoàn tác."
            @confirm="handleDelete"
            type="error"
        />
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { AdminBreadcrumbs, AdminTable, AdminPagination, AdminConfirm } from '@/components/common';
import { dichVuDanhGia } from '@/services/admin/dichVuDanhGia';
import { useNotifications } from '@/services/notificationService';
import dayjs from 'dayjs';

const { addNotification } = useNotifications();

const breadcrumbs = [
    { title: 'Quản lý đánh giá', disabled: true }
];

const headers = [
    { text: 'Ngày tạo', value: 'ngayTao', width: '120px' },
    { text: 'Khách hàng', value: 'khachHang', width: '200px' },
    { text: 'Sản phẩm', value: 'sanPham', width: '250px' },
    { text: 'Đánh giá', value: 'diemDanhGia', width: '150px' },
    { text: 'Nội dung', value: 'noiDung' },
    { text: 'Trạng thái', value: 'trangThai', width: '120px', align: 'center' },
    { text: 'Thao tác', value: 'actions', width: '150px', align: 'center', sortable: false }
];

const statusOptions = [
    { title: 'Tất cả', value: null },
    { title: 'Chờ duyệt', value: 'PENDING' },
    { title: 'Đã duyệt', value: 'APPROVED' },
    { title: 'Bị từ chối', value: 'REJECTED' },
    { title: 'Spam', value: 'SPAM' }
];

const items = ref([]);
const loading = ref(false);
const page = ref(1);
const size = ref(10);
const totalPages = ref(0);
const totalItems = ref(0);

const filter = ref({
    keyword: '',
    trangThai: null
});

const deleteDialog = ref(false);
const itemToDelete = ref(null);

const fetchData = async () => {
    loading.value = true;
    try {
        const response = await dichVuDanhGia.getAll({
            page: page.value - 1,
            size: size.value,
            keyword: filter.value.keyword,
            trangThai: filter.value.trangThai
        });
        if (response?.data) {
            items.value = response.data.content || [];
            totalPages.value = response.data.totalPages || 0;
            totalItems.value = response.data.totalElements || 0;
        }
    } catch (error) {
        addNotification('Lỗi khi tải danh sách đánh giá', 'error');
    } finally {
        loading.value = false;
    }
};

const handleSearch = () => {
    page.value = 1;
    fetchData();
};

const handlePageChange = (newPage) => {
    page.value = newPage;
    fetchData();
};

const updateStatus = async (item, status) => {
    try {
        await dichVuDanhGia.updateStatus(item.id, status);
        addNotification('Cập nhật trạng thái thành công', 'success');
        fetchData();
    } catch (error) {
        addNotification('Lỗi khi cập nhật trạng thái', 'error');
    }
};

const confirmDelete = (item) => {
    itemToDelete.value = item;
    deleteDialog.value = true;
};

const handleDelete = async () => {
    if (!itemToDelete.value) return;
    try {
        await dichVuDanhGia.delete(itemToDelete.value.id);
        addNotification('Xóa đánh giá thành công', 'success');
        fetchData();
    } catch (error) {
        addNotification('Lỗi khi xóa đánh giá', 'error');
    } finally {
        deleteDialog.value = false;
        itemToDelete.value = null;
    }
};

const getStatusColor = (status) => {
    switch (status) {
        case 'APPROVED': return 'success';
        case 'REJECTED': return 'error';
        case 'SPAM': return 'warning';
        case 'PENDING': default: return 'info';
    }
};

const getStatusText = (status) => {
    switch (status) {
        case 'APPROVED': return 'Đã duyệt';
        case 'REJECTED': return 'Từ chối';
        case 'SPAM': return 'Spam';
        case 'PENDING': default: return 'Chờ duyệt';
    }
};

const formatDate = (date) => {
    if (!date) return '';
    return dayjs(date).format('DD/MM/YYYY HH:mm');
};

onMounted(() => {
    fetchData();
});
</script>

<style scoped>
.gap-2 {
    gap: 8px;
}
</style>
