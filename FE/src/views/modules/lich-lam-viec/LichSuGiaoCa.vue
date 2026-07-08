<script setup>
import { ref, onMounted } from 'vue';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { useNotifications } from '@/services/notificationService';
import { useUIStore } from '@/stores/ui';
import { formatDateTime } from '@/utils/formatters';

const uiStore = useUIStore();
const { addNotification } = useNotifications();

const listGiaoCa = ref([]);
const loading = ref(false);

const headers = [
    { title: 'Nhân viên', key: 'nhanVienTen' },
    { title: 'Mã ca', key: 'id' },
    { title: 'Trạng thái', key: 'trangThai' },
    { title: 'Thời gian mở', key: 'thoiGianMoCa' },
    { title: 'Thời gian chốt', key: 'thoiGianChotCa' },
    { title: 'Tiền mặt đầu ca', key: 'tienBanDau', align: 'end' },
    { title: 'Doanh thu ca', key: 'tongDoanhThu', align: 'end' },
    { title: 'Tiền mặt chốt ca', key: 'tienThucTe', align: 'end' },
    { title: 'Lệch', key: 'chenhLech', align: 'end' },
    { title: 'Người nhận ca', key: 'nhanVienNhanCaTen' },
];

const fetchListGiaoCa = async () => {
    loading.value = true;
    try {
        const res = await dichVuGiaoCa.getAllLichSu();
        listGiaoCa.value = res?.data || res || [];
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi tải danh sách giao ca', color: 'error' });
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    uiStore.setBreadcrumbs([
        { title: 'Giao ca', disabled: false, href: '/admin/giao-ca' },
        { title: 'Lịch sử giao ca', disabled: true }
    ]);
    fetchListGiaoCa();
});

const getStatusColor = (status) => {
    if (status === 'OPEN') return 'success';
    if (status === 'CLOSED') return 'grey';
    return 'primary';
};
const getStatusLabel = (status) => {
    if (status === 'OPEN') return 'Đang mở';
    if (status === 'CLOSED') return 'Đã chốt';
    return status;
};

const formatDate = (dateNum) => {
    if (!dateNum) return '--';
    return formatDateTime(dateNum);
};

const formatCurrency = (val) => {
    return Number(val || 0).toLocaleString() + ' đ';
};

const getChenhLech = (item) => {
    const expected = (item.tienBanDau || 0) + (item.tongDoanhThu || 0);
    return (item.tienThucTe || 0) - expected;
};

const getChenhLechColor = (val) => {
    if (val > 0) return 'text-info';
    if (val < 0) return 'text-error';
    return 'text-success';
};
</script>

<template>
    <v-container fluid class="pa-4">
        <v-card class="rounded-lg shadow-sm mb-4 border">
            <v-card-title class="pa-4 border-b font-weight-bold d-flex align-center">
                <v-icon color="primary" class="mr-2">mdi-history</v-icon>
                Lịch Sử Giao Ca
                <v-spacer></v-spacer>
                <v-btn color="primary" variant="tonal" prepend-icon="mdi-refresh" @click="fetchListGiaoCa" :loading="loading">Làm mới</v-btn>
            </v-card-title>

            <v-data-table
                :headers="headers"
                :items="listGiaoCa"
                :loading="loading"
                class="elevation-0 rounded-lg custom-table"
                hover
                density="comfortable"
            >
                <template v-slot:item.trangThai="{ item }">
                    <v-chip size="small" :color="getStatusColor(item.trangThai)" class="font-weight-bold" variant="flat">
                        {{ getStatusLabel(item.trangThai) }}
                    </v-chip>
                </template>
                <template v-slot:item.thoiGianMoCa="{ item }">
                    {{ formatDate(item.thoiGianMoCa) }}
                </template>
                <template v-slot:item.thoiGianChotCa="{ item }">
                    {{ formatDate(item.thoiGianChotCa) }}
                </template>
                <template v-slot:item.tienBanDau="{ item }">
                    {{ formatCurrency(item.tienBanDau) }}
                </template>
                <template v-slot:item.tongDoanhThu="{ item }">
                    <span class="text-success font-weight-bold">{{ formatCurrency(item.tongDoanhThu) }}</span>
                </template>
                <template v-slot:item.tienThucTe="{ item }">
                    {{ formatCurrency(item.tienThucTe) }}
                </template>
                <template v-slot:item.chenhLech="{ item }">
                    <span :class="['font-weight-bold', getChenhLechColor(getChenhLech(item))]">
                        {{ getChenhLech(item) > 0 ? '+' : '' }}{{ formatCurrency(getChenhLech(item)) }}
                    </span>
                </template>
                <template v-slot:item.nhanVienNhanCaTen="{ item }">
                    {{ item.nhanVienNhanCaTen || '--' }}
                </template>
                
                <template v-slot:no-data>
                    <div class="pa-5 text-center">
                        <v-icon size="48" color="grey-lighten-2" class="mb-3">mdi-inbox-outline</v-icon>
                        <div class="text-grey-darken-1">Không có dữ liệu giao ca</div>
                    </div>
                </template>
            </v-data-table>
        </v-card>
    </v-container>
</template>

<style scoped>
.custom-table :deep(th) {
    background-color: #f8fafc !important;
    font-weight: 600 !important;
    color: #334155 !important;
    white-space: nowrap;
}
.custom-table :deep(td) {
    border-bottom: 1px solid #f1f5f9;
}
</style>
