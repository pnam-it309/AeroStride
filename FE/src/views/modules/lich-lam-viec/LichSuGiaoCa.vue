<script setup>
import { ref, onMounted } from 'vue';
import { AdminTable } from '@/components/common';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { useNotifications } from '@/services/notificationService';
import { useUIStore } from '@/stores/ui';
import { formatDateTime, formatCurrency } from '@/utils/formatters';

const uiStore = useUIStore();
const { addNotification } = useNotifications();

const listGiaoCa = ref([]);
const loading = ref(false);

const headers = [
    { text: 'Mã ca', align: 'center', width: '80px' },
    { text: 'Nhân viên mở', align: 'start' },
    { text: 'Trạng thái', align: 'center' },
    { text: 'Thời gian mở', align: 'center' },
    { text: 'Thời gian chốt', align: 'center' },
    { text: 'Tiền mặt đầu ca', align: 'end' },
    { text: 'Doanh thu ca', align: 'end' },
    { text: 'Tiền mặt chốt ca', align: 'end' },
    { text: 'Chênh lệch', align: 'end' },
    { text: 'Người nhận ca', align: 'start' }
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
        <AdminTable
            title="Lịch Sử Giao Ca"
            :headers="headers"
            :items="listGiaoCa"
            :loading="loading"
            :show-add-button="false"
            class="rounded-lg border elevation-1"
        >
            <template #extra-actions>
                <v-btn color="primary" variant="tonal" prepend-icon="mdi-refresh" @click="fetchListGiaoCa" :loading="loading">
                    Làm mới
                </v-btn>
            </template>
            <template #row="{ item }">
                <tr class="data-row">
                    <td class="data-cell text-center">#{{ item.id }}</td>
                    <td class="data-cell font-weight-medium">{{ item.nhanVienTen || 'N/A' }}</td>
                    <td class="data-cell text-center">
                        <v-chip size="small" :color="getStatusColor(item.trangThai)" class="font-weight-bold" variant="flat">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>
                    <td class="data-cell text-center text-slate-500">{{ formatDate(item.thoiGianMoCa) }}</td>
                    <td class="data-cell text-center text-slate-500">{{ formatDate(item.thoiGianChotCa) }}</td>
                    <td class="data-cell text-right">{{ formatCurrency(item.tienBanDau) }}</td>
                    <td class="data-cell text-right text-success font-weight-bold">{{ formatCurrency(item.tongDoanhThu) }}</td>
                    <td class="data-cell text-right">{{ formatCurrency(item.tienThucTe) }}</td>
                    <td class="data-cell text-right">
                        <span :class="['font-weight-bold', getChenhLechColor(getChenhLech(item))]">
                            {{ getChenhLech(item) > 0 ? '+' : '' }}{{ formatCurrency(getChenhLech(item)) }}
                        </span>
                    </td>
                    <td class="data-cell font-weight-medium">{{ item.nhanVienNhanCaTen || '--' }}</td>
                </tr>
            </template>
        </AdminTable>
    </v-container>
</template>
